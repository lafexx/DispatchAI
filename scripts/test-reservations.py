#!/usr/bin/env python3
"""Additive live-stack checks. Requires Python 3.10+, running upgraded APIs, Kafka."""

import json
import os
import time
import uuid
from concurrent.futures import ThreadPoolExecutor
from urllib.error import HTTPError
from urllib.request import Request, urlopen

URLS = {
    name: os.getenv(f"{name.upper()}_URL", f"http://localhost:{port}").rstrip("/")
    for name, port in [
        ("customer", 8084),
        ("fleet", 8082),
        ("dispatch", 8083),
        ("delivery", 8081),
    ]
}


def call(service, method, path, body=None):
    request = Request(
        URLS[service] + path,
        method=method,
        data=json.dumps(body).encode() if body is not None else None,
        headers={"Content-Type": "application/json"},
    )
    try:
        with urlopen(request, timeout=15) as response:
            data = response.read()
            return response.status, json.loads(data) if data else None
    except HTTPError as error:
        data = error.read().decode()
        try:
            data = json.loads(data)
        except ValueError:
            pass
        return error.code, data


def ok(service, method, path, body=None):
    status, data = call(service, method, path, body)
    assert 200 <= status < 300, (service, method, path, status, data)
    return data


def wait(service, path, predicate):
    deadline = time.monotonic() + 30
    while time.monotonic() < deadline:
        status, data = call(service, "GET", path)
        if status == 200 and predicate(data):
            return data
        time.sleep(0.5)
    raise AssertionError(f"Timed out waiting for {service}{path}")


def main():
    tag = uuid.uuid4().hex[:8]
    customer = ok(
        "customer",
        "POST",
        "/api/customers",
        {
            "name": f"Reservation test {tag}",
            "email": f"reservation-{tag}@example.com",
            "phone": "0400000000",
        },
    )["id"]

    def driver():
        value = ok(
            "fleet", "POST", "/api/drivers", {"name": f"Reservation test {tag}"}
        )["id"]
        ok("fleet", "POST", f"/api/drivers/{value}/availability", {"available": True})
        return value

    def delivery():
        return ok(
            "delivery",
            "POST",
            "/api/deliveries",
            {
                "customerId": customer,
                "pickupAddress": "12 King St",
                "destinationAddress": "45 George St",
            },
        )["id"]

    print("Two Dispatch requests competing for one driver")
    resource = driver()
    deliveries = [delivery(), delivery()]
    bodies = [
        {"deliveryId": item, "driverId": resource, "vehicleId": None}
        for item in deliveries
    ]
    with ThreadPoolExecutor(max_workers=2) as pool:
        results = list(
            pool.map(
                lambda body: call(
                    "dispatch", "POST", "/api/dispatch/assignments", body
                ),
                bodies,
            )
        )
    assert sorted(status for status, _ in results) == [201, 409], results
    index = next(i for i, result in enumerate(results) if result[0] == 201)
    assignment = results[index][1]
    bound = bodies[index]
    assert (
        ok("fleet", "GET", f"/api/drivers/{resource}")["availability"] == "UNAVAILABLE"
    )
    repeated = ok("dispatch", "POST", "/api/dispatch/assignments", bound)
    assert repeated["id"] == assignment["id"]
    wait(
        "delivery",
        f"/api/deliveries/{bound['deliveryId']}",
        lambda data: data["status"] == "ASSIGNED",
    )
    assert (
        ok("fleet", "GET", f"/api/reservations/{assignment['id']}")["status"]
        == "CONFIRMED"
    )
    ok("delivery", "POST", f"/api/deliveries/{bound['deliveryId']}/start")
    ok("delivery", "POST", f"/api/deliveries/{bound['deliveryId']}/complete")
    wait(
        "fleet",
        f"/api/drivers/{resource}",
        lambda data: data["availability"] == "AVAILABLE",
    )
    wait(
        "dispatch",
        f"/api/dispatch/assignments/{assignment['id']}",
        lambda data: data["status"] == "COMPLETED",
    )

    print("Two driver/vehicle pairs competing for one vehicle")
    vehicle = ok(
        "fleet",
        "POST",
        "/api/vehicles",
        {"registration": f"RES-{tag}", "type": "VAN", "capacityKg": 1200},
    )["id"]
    ok("fleet", "POST", f"/api/vehicles/{vehicle}/availability", {"available": True})
    ids = [str(uuid.uuid4()), str(uuid.uuid4())]
    requests = [
        {"deliveryId": delivery(), "driverId": driver(), "vehicleId": vehicle}
        for _ in ids
    ]
    with ThreadPoolExecutor(max_workers=2) as pool:
        results = list(
            pool.map(
                lambda pair: call(
                    "fleet", "PUT", f"/api/reservations/{pair[0]}", pair[1]
                ),
                zip(ids, requests),
            )
        )
    assert sorted(status for status, _ in results) == [200, 409], results
    index = next(i for i, result in enumerate(results) if result[0] == 200)
    same = ok("fleet", "PUT", f"/api/reservations/{ids[index]}", requests[index])
    assert same["assignmentId"] == ids[index]
    ok("fleet", "POST", f"/api/reservations/{ids[index]}/release", requests[index])
    assert ok("fleet", "GET", f"/api/vehicles/{vehicle}")["available"] is True

    print("Release before acquisition prevents a delayed reservation")
    token = str(uuid.uuid4())
    request = {"deliveryId": delivery(), "driverId": resource, "vehicleId": None}
    ok("fleet", "POST", f"/api/reservations/{token}/release", request)
    assert call("fleet", "PUT", f"/api/reservations/{token}", request)[0] == 409
    assert ok("fleet", "GET", f"/api/drivers/{resource}")["availability"] == "AVAILABLE"

    print("Confirmed holds resist compensation and close on delivery cancellation")
    token = str(uuid.uuid4())
    request = {"deliveryId": delivery(), "driverId": resource, "vehicleId": None}
    ok("fleet", "PUT", f"/api/reservations/{token}", request)
    ok("fleet", "POST", f"/api/reservations/{token}/confirm", request)
    assert (
        call("fleet", "POST", f"/api/reservations/{token}/release", request)[0] == 409
    )
    ok(
        "delivery",
        "POST",
        f"/api/deliveries/{request['deliveryId']}/cancel",
        {"reason": "CUSTOMER_REQUEST", "notes": "Reservation race test"},
    )
    wait(
        "fleet", f"/api/reservations/{token}", lambda data: data["status"] == "RELEASED"
    )
    assert ok("fleet", "GET", f"/api/drivers/{resource}")["availability"] == "AVAILABLE"

    print("Cancellation racing a Dispatch request eventually releases resources")
    target = delivery()
    with ThreadPoolExecutor(max_workers=2) as pool:
        assigned = pool.submit(
            call,
            "dispatch",
            "POST",
            "/api/dispatch/assignments",
            {"deliveryId": target, "driverId": resource, "vehicleId": None},
        )
        cancelled = pool.submit(
            call,
            "delivery",
            "POST",
            f"/api/deliveries/{target}/cancel",
            {"reason": "CUSTOMER_REQUEST", "notes": "Concurrent cancellation"},
        )
        assign_result, cancel_result = assigned.result(), cancelled.result()
    assert assign_result[0] in (201, 409), assign_result
    assert cancel_result[0] == 200, cancel_result
    wait(
        "delivery",
        f"/api/deliveries/{target}",
        lambda data: data["status"] == "CANCELLED",
    )
    wait(
        "fleet",
        f"/api/drivers/{resource}",
        lambda data: data["availability"] == "AVAILABLE",
    )
    if assign_result[0] == 201:
        wait(
            "dispatch",
            f"/api/dispatch/assignments/{assign_result[1]['id']}",
            lambda data: data["status"] == "CANCELLED",
        )
    print("Reservation live-stack checks passed. Demo records remain for inspection.")


if __name__ == "__main__":
    main()
