#!/usr/bin/env bash
set -euo pipefail

# Additive smoke test: creates its own customer, driver, vehicle, and deliveries.
# Requires an already-running upgraded stack, curl, and jq. No LLM requests.
CUSTOMER_URL="${CUSTOMER_URL:-http://localhost:8084}"
FLEET_URL="${FLEET_URL:-http://localhost:8082}"
DELIVERY_URL="${DELIVERY_URL:-http://localhost:8081}"
DISPATCH_URL="${DISPATCH_URL:-http://localhost:8083}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-30}"

for required in curl jq; do
  command -v "$required" >/dev/null || { echo "Missing $required" >&2; exit 1; }
done
[[ "$TIMEOUT_SECONDS" =~ ^[1-9][0-9]*$ ]] || { echo "TIMEOUT_SECONDS must be a positive integer" >&2; exit 1; }

post() {
  if [[ $# -eq 2 ]]; then
    curl --connect-timeout 3 --max-time 10 --fail-with-body -sS -X POST "$1" -H 'Content-Type: application/json' -d "$2"
  else
    curl --connect-timeout 3 --max-time 10 --fail-with-body -sS -X POST "$1"
  fi
}
get() { curl --connect-timeout 3 --max-time 10 --fail-with-body -sS "$1"; }
wait_for() {
  local url="$1" expression="$2" deadline=$((SECONDS + TIMEOUT_SECONDS)) response
  while ((SECONDS < deadline)); do
    if response="$(get "$url")" && jq -e "$expression" <<<"$response" >/dev/null; then
      return 0
    fi
    sleep 1
  done
  echo "Timed out waiting for $url to satisfy: $expression" >&2
  exit 1
}

task_run="$(date +%s)-$$"
customer_id="$(post "$CUSTOMER_URL/api/customers" "$(jq -nc --arg tag "$task_run" '{name:("Lifecycle test " + $tag),email:("lifecycle-" + $tag + "@example.com"),phone:"0400000000"}')" | jq -er '.id')"
driver_id="$(post "$FLEET_URL/api/drivers" '{"name":"Lifecycle test driver"}' | jq -er '.id')"
vehicle_id="$(post "$FLEET_URL/api/vehicles" "$(jq -nc --arg registration "TEST-$task_run" '{registration:$registration,type:"VAN",capacityKg:1200}')" | jq -er '.id')"
post "$FLEET_URL/api/drivers/$driver_id/availability" '{"available":true}' >/dev/null
post "$FLEET_URL/api/vehicles/$vehicle_id/availability" '{"available":true}' >/dev/null

create_delivery() {
  post "$DELIVERY_URL/api/deliveries" "$(jq -nc --arg customer "$customer_id" '{customerId:$customer,pickupAddress:"12 King St",destinationAddress:"45 George St",parcelWeightKg:5}')" | jq -er '.id'
}
assign_delivery() {
  local delivery="$1"
  post "$DISPATCH_URL/api/dispatch/assignments" "$(jq -nc --arg delivery "$delivery" --arg driver "$driver_id" --arg vehicle "$vehicle_id" '{deliveryId:$delivery,driverId:$driver,vehicleId:$vehicle}')" >/dev/null
  wait_for "$DELIVERY_URL/api/deliveries/$delivery" '.status == "ASSIGNED"'
  wait_for "$FLEET_URL/api/drivers/$driver_id" '.availability == "UNAVAILABLE"'
  wait_for "$FLEET_URL/api/vehicles/$vehicle_id" '.available == false'
}
wait_released() {
  wait_for "$FLEET_URL/api/drivers/$driver_id" '.availability == "AVAILABLE"'
  wait_for "$FLEET_URL/api/vehicles/$vehicle_id" '.available == true'
}

echo 'Completion closes the assignment and releases driver/vehicle'
first="$(create_delivery)"
assign_delivery "$first"
post "$DELIVERY_URL/api/deliveries/$first/start" >/dev/null
post "$DELIVERY_URL/api/deliveries/$first/complete" >/dev/null
wait_for "$DISPATCH_URL/api/dispatch/deliveries/$first/assignment" '.status == "COMPLETED" and .completedAt != null'
wait_released

echo 'The same driver/vehicle can be reused, then released on cancellation'
second="$(create_delivery)"
assign_delivery "$second"
post "$DELIVERY_URL/api/deliveries/$second/cancel" '{"reason":"CUSTOMER_REQUEST","notes":"Lifecycle test"}' >/dev/null
wait_for "$DISPATCH_URL/api/dispatch/deliveries/$second/assignment" '.status == "CANCELLED" and .cancelledAt != null'
wait_released

echo 'An unassigned cancellation leaves the fleet unchanged'
third="$(create_delivery)"
post "$DELIVERY_URL/api/deliveries/$third/cancel" '{"reason":"CUSTOMER_REQUEST","notes":"Never assigned"}' >/dev/null
wait_for "$DELIVERY_URL/api/deliveries/$third" '.status == "CANCELLED"'
wait_released

echo 'Manual availability changes cannot release busy resources'
fourth="$(create_delivery)"
assign_delivery "$fourth"
post "$FLEET_URL/api/drivers/$driver_id/availability" '{"available":true}' >/dev/null
post "$FLEET_URL/api/vehicles/$vehicle_id/availability" '{"available":true}' >/dev/null
wait_for "$FLEET_URL/api/drivers/$driver_id" '.availability == "UNAVAILABLE"'
wait_for "$FLEET_URL/api/vehicles/$vehicle_id" '.available == false'
post "$DELIVERY_URL/api/deliveries/$fourth/start" >/dev/null
post "$DELIVERY_URL/api/deliveries/$fourth/complete" >/dev/null
wait_for "$DISPATCH_URL/api/dispatch/deliveries/$fourth/assignment" '.status == "COMPLETED"'
wait_released

echo "Lifecycle smoke test passed. Customer=$customer_id Driver=$driver_id Vehicle=$vehicle_id"
