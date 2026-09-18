#!/usr/bin/env bash
set -euo pipefail

# DispatchAI end-to-end test-data + smoke-test script.
#
# Requirements:
#   curl
#   jq
#
# Optional environment overrides:
#   CUSTOMER_URL=http://localhost:8084
#   FLEET_URL=http://localhost:8082
#   DELIVERY_URL=http://localhost:8081
#   ANALYTICS_URL=http://localhost:8085
#   AGENT_URL=http://localhost:8086
#
# Usage:
#   chmod +x test-dispatchai.sh
#   ./test-dispatchai.sh
#
# The script exports/captures IDs internally and prints them at the end.

CUSTOMER_URL="${CUSTOMER_URL:-http://localhost:8084}"
FLEET_URL="${FLEET_URL:-http://localhost:8082}"
DELIVERY_URL="${DELIVERY_URL:-http://localhost:8081}"
ANALYTICS_URL="${ANALYTICS_URL:-http://localhost:8085}"
AGENT_URL="${AGENT_URL:-http://localhost:8086}"

ASSIGNMENT_TIMEOUT_SECONDS="${ASSIGNMENT_TIMEOUT_SECONDS:-10}"
ANALYTICS_WAIT_SECONDS="${ANALYTICS_WAIT_SECONDS:-3}"

for cmd in curl jq; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERROR: '$cmd' is required but was not found." >&2
    exit 1
  fi
done

section() {
  printf '\n\033[1;34m== %s ==\033[0m\n' "$1"
}

die() {
  echo "ERROR: $*" >&2
  exit 1
}

post_json() {
  local url="$1"
  local body="$2"

  curl --fail-with-body -sS \
    -X POST "$url" \
    -H 'Content-Type: application/json' \
    -d "$body"
}

post_empty() {
  local url="$1"

  curl --fail-with-body -sS \
    -X POST "$url"
}

get_json() {
  local url="$1"

  curl --fail-with-body -sS "$url"
}

wait_for_assignment() {
  local delivery_id="$1"
  local elapsed=0
  local response
  local driver_id

  while ((elapsed < ASSIGNMENT_TIMEOUT_SECONDS)); do
    response="$(get_json "$DELIVERY_URL/api/deliveries/$delivery_id")"
    driver_id="$(jq -r '.assignedDriverId // empty' <<<"$response")"

    if [[ -n "$driver_id" ]]; then
      printf '%s\n' "$driver_id"
      return 0
    fi

    sleep 1
    ((elapsed += 1))
  done

  die "Delivery $delivery_id was not assigned within ${ASSIGNMENT_TIMEOUT_SECONDS}s"
}

RUN_ID="$(date +%s)-$$"

section "Configuration"
cat <<EOF
CUSTOMER_URL  = $CUSTOMER_URL
FLEET_URL     = $FLEET_URL
DELIVERY_URL  = $DELIVERY_URL
ANALYTICS_URL = $ANALYTICS_URL
AGENT_URL     = $AGENT_URL
RUN_ID        = $RUN_ID
EOF

# ---------------------------------------------------------------------------
# Customer
# ---------------------------------------------------------------------------

section "Create customer"

CUSTOMER_RESPONSE="$(
  post_json "$CUSTOMER_URL/api/customers" "{
    \"name\": \"Bob Test $RUN_ID\",
    \"email\": \"bob+$RUN_ID@example.com\",
    \"phone\": \"1234567890\"
  }"
)"

echo "$CUSTOMER_RESPONSE" | jq

CUSTOMER_ID="$(jq -r '.id' <<<"$CUSTOMER_RESPONSE")"
[[ -n "$CUSTOMER_ID" && "$CUSTOMER_ID" != "null" ]] ||
  die "Customer creation did not return an ID"

# ---------------------------------------------------------------------------
# Drivers
# ---------------------------------------------------------------------------

section "Create drivers"

create_driver() {
  local name="$1"
  local response

  response="$(
    post_json "$FLEET_URL/api/drivers" "{
      \"name\": \"$name\"
    }"
  )"

  echo "$response" | jq >&2
  jq -r '.id' <<<"$response"
}

DRIVER_1_ID="$(create_driver "Driver 1 - $RUN_ID")"
DRIVER_2_ID="$(create_driver "Driver 2 - $RUN_ID")"
DRIVER_3_ID="$(create_driver "Driver 3 - $RUN_ID")"
DRIVER_4_ID="$(create_driver "Driver 4 - $RUN_ID")"
DRIVER_5_ID="$(create_driver "Driver 5 - $RUN_ID")"

section "Mark drivers available"

for driver_id in "$DRIVER_1_ID" "$DRIVER_2_ID" "$DRIVER_3_ID" "$DRIVER_4_ID" "$DRIVER_5_ID"; do
  post_json \
    "$FLEET_URL/api/drivers/$driver_id/availability" \
    '{"available":true}' | jq
done

# ---------------------------------------------------------------------------
# Delivery 1: completed
# ---------------------------------------------------------------------------

section "Delivery 1: create -> assigned -> in transit -> completed"

DELIVERY_1_RESPONSE="$(
  post_json "$DELIVERY_URL/api/deliveries" "{
    \"customerId\": \"$CUSTOMER_ID\",
    \"pickupAddress\": \"1 Pickup St\",
    \"destinationAddress\": \"10 Market St\"
  }"
)"

echo "$DELIVERY_1_RESPONSE" | jq
DELIVERY_1_ID="$(jq -r '.id' <<<"$DELIVERY_1_RESPONSE")"

ASSIGNED_DRIVER_1="$(wait_for_assignment "$DELIVERY_1_ID")"
echo "Assigned driver: $ASSIGNED_DRIVER_1"

post_empty "$DELIVERY_URL/api/deliveries/$DELIVERY_1_ID/start" | jq
post_empty "$DELIVERY_URL/api/deliveries/$DELIVERY_1_ID/complete" | jq

# ---------------------------------------------------------------------------
# Delivery 2: delayed and in transit
# ---------------------------------------------------------------------------

section "Delivery 2: create -> assigned -> in transit -> delayed"

DELIVERY_2_RESPONSE="$(
  post_json "$DELIVERY_URL/api/deliveries" "{
    \"customerId\": \"$CUSTOMER_ID\",
    \"pickupAddress\": \"20 King St\",
    \"destinationAddress\": \"40 George St\"
  }"
)"

echo "$DELIVERY_2_RESPONSE" | jq
DELIVERY_2_ID="$(jq -r '.id' <<<"$DELIVERY_2_RESPONSE")"

ASSIGNED_DRIVER_2="$(wait_for_assignment "$DELIVERY_2_ID")"
echo "Assigned driver: $ASSIGNED_DRIVER_2"

post_empty "$DELIVERY_URL/api/deliveries/$DELIVERY_2_ID/start" | jq

post_json \
  "$DELIVERY_URL/api/deliveries/$DELIVERY_2_ID/delay" \
  '{
    "delayMinutes": 25,
    "reason": "Heavy traffic"
  }' | jq

# ---------------------------------------------------------------------------
# Delivery 3: cancelled
# ---------------------------------------------------------------------------

section "Delivery 3: create -> assigned -> cancelled"

DELIVERY_3_RESPONSE="$(
  post_json "$DELIVERY_URL/api/deliveries" "{
    \"customerId\": \"$CUSTOMER_ID\",
    \"pickupAddress\": \"5 Harbour St\",
    \"destinationAddress\": \"90 Pitt St\"
  }"
)"

echo "$DELIVERY_3_RESPONSE" | jq
DELIVERY_3_ID="$(jq -r '.id' <<<"$DELIVERY_3_RESPONSE")"

ASSIGNED_DRIVER_3="$(wait_for_assignment "$DELIVERY_3_ID")"
echo "Assigned driver: $ASSIGNED_DRIVER_3"

post_json \
  "$DELIVERY_URL/api/deliveries/$DELIVERY_3_ID/cancel" \
  '{
    "reason": "CUSTOMER_REQUEST",
    "notes": "Customer cancelled the delivery"
  }' | jq

# ---------------------------------------------------------------------------
# Give Kafka Streams a short period to materialize the latest events.
# ---------------------------------------------------------------------------

section "Wait for Analytics projection"
echo "Waiting ${ANALYTICS_WAIT_SECONDS}s..."
sleep "$ANALYTICS_WAIT_SECONDS"

# ---------------------------------------------------------------------------
# Analytics smoke tests
# ---------------------------------------------------------------------------

section "Analytics summary"

SUMMARY="$(
  get_json "$ANALYTICS_URL/api/analytics/deliveries/summary"
)"
echo "$SUMMARY" | jq

section "Analytics: completed delivery"
get_json "$ANALYTICS_URL/api/analytics/deliveries/$DELIVERY_1_ID" | jq

section "Analytics: delayed delivery"
get_json "$ANALYTICS_URL/api/analytics/deliveries/$DELIVERY_2_ID" | jq

section "Analytics: cancelled delivery"
get_json "$ANALYTICS_URL/api/analytics/deliveries/$DELIVERY_3_ID" | jq

section "Analytics: completed driver's metrics"
get_json \
  "$ANALYTICS_URL/api/analytics/deliveries/drivers/$ASSIGNED_DRIVER_1" |
  jq

# Basic projection assertions. These are >= checks because the Analytics
# state store may contain records from previous test runs.
TOTAL="$(jq -r '.total' <<<"$SUMMARY")"
COMPLETED="$(jq -r '.completed' <<<"$SUMMARY")"
IN_TRANSIT="$(jq -r '.inTransit' <<<"$SUMMARY")"
CANCELLED="$(jq -r '.cancelled' <<<"$SUMMARY")"
DELAYED="$(jq -r '.delayed' <<<"$SUMMARY")"

((TOTAL >= 3)) || die "Expected analytics total >= 3, got $TOTAL"
((COMPLETED >= 1)) || die "Expected completed >= 1, got $COMPLETED"
((IN_TRANSIT >= 1)) || die "Expected inTransit >= 1, got $IN_TRANSIT"
((CANCELLED >= 1)) || die "Expected cancelled >= 1, got $CANCELLED"
((DELAYED >= 1)) || die "Expected delayed >= 1, got $DELAYED"

echo "Analytics assertions passed."

# ---------------------------------------------------------------------------
# Operations Agent smoke tests
# ---------------------------------------------------------------------------

section "Operations Assistant Agent: operations summary"

post_json \
  "$AGENT_URL/api/agent/operations/query" \
  '{"message":"Give me a summary of the current delivery operation."}' |
  jq

section "Operations Assistant Agent: operational issues"

post_json \
  "$AGENT_URL/api/agent/operations/query" \
  '{"message":"Are there any operational issues that need attention right now?"}' |
  jq

section "Operations Assistant Agent: rates"

post_json \
  "$AGENT_URL/api/agent/operations/query" \
  '{"message":"What are the current completion, cancellation, and delay rates?"}' |
  jq

section "Operations Assistant Agent: inspect specific delayed delivery"

post_json \
  "$AGENT_URL/api/agent/operations/query" \
  "{
    \"message\": \"What is happening with delivery $DELIVERY_2_ID?\"
  }" |
  jq

section "Operations Assistant Agent: inspect assigned driver"

post_json \
  "$AGENT_URL/api/agent/operations/query" \
  "{
    \"message\": \"What is the current status of driver $ASSIGNED_DRIVER_2?\"
  }" |
  jq

section "Operations Assistant Agent: chained delivery -> driver lookup"

post_json \
  "$AGENT_URL/api/agent/operations/query" \
  "{
    \"message\": \"Who is assigned to delivery $DELIVERY_2_ID and what is that driver's current availability?\"
  }" |
  jq

section "Dispatch Advisor Agent: recommend driver"

post_json \
  "$AGENT_URL/api/agent/dispatch/advice" \
  "{
    \"message\": \"Which available driver would you recommend for delivery $DELIVERY_2_ID?\"
  }" |
  jq

# ---------------------------------------------------------------------------
# Final IDs for manual follow-up
# ---------------------------------------------------------------------------

section "Created test IDs"

cat <<EOF
CUSTOMER_ID=$CUSTOMER_ID

DRIVER_1_ID=$DRIVER_1_ID
DRIVER_2_ID=$DRIVER_2_ID
DRIVER_3_ID=$DRIVER_3_ID

DELIVERY_1_ID=$DELIVERY_1_ID
DELIVERY_2_ID=$DELIVERY_2_ID
DELIVERY_3_ID=$DELIVERY_3_ID

ASSIGNED_DRIVER_1=$ASSIGNED_DRIVER_1
ASSIGNED_DRIVER_2=$ASSIGNED_DRIVER_2
ASSIGNED_DRIVER_3=$ASSIGNED_DRIVER_3
EOF

section "Done"
echo "DispatchAI test data populated and smoke tests completed."
