#!/usr/bin/env python3
"""Standard-library, opt-in live smoke test. See README.transactional-outbox.md."""

import argparse
import json
import time
import uuid
from pathlib import Path
from urllib.request import Request, urlopen


def request(base, path, body=None):
    data = None if body is None else json.dumps(body).encode()
    req = Request(
        base.rstrip("/") + path, data=data, headers={"Content-Type": "application/json"}
    )
    with urlopen(req, timeout=10) as response:
        return json.load(response)


def queued(args):
    if args.state.exists():
        raise SystemExit(
            "Checkpoint exists; use verify or drain, or choose a different --state path"
        )
    customer = request(
        args.customer,
        "/api/customers",
        {
            "name": "Outbox smoke test",
            "email": f"outbox-{uuid.uuid4()}@example.test",
            "phone": None,
        },
    )
    delivery = request(
        args.delivery,
        "/api/deliveries",
        {
            "customerId": customer["id"],
            "pickupAddress": "Outbox test pickup",
            "destinationAddress": "Outbox test destination",
        },
    )
    cancelled = request(
        args.delivery,
        f"/api/deliveries/{delivery['id']}/cancel",
        {"reason": "CUSTOMER_REQUEST", "notes": "Transactional outbox smoke test"},
    )
    assert cancelled["status"] == "CANCELLED"
    status = request(
        args.delivery, f"/api/messaging/outbox?aggregateId={delivery['id']}"
    )
    events = status["oldestPending"]
    assert len(events) == 2, (
        "Start with compose.outbox-test.yaml so both events remain queued"
    )
    assert [e["binding"] for e in events] == [
        "deliveryCreated-out-0",
        "deliveryCancelled-out-0",
    ]
    state = {
        "deliveryId": delivery["id"],
        "customerId": customer["id"],
        "events": [
            {
                "eventId": e["eventId"],
                "binding": e["binding"],
                "createdAt": e["createdAt"],
            }
            for e in events
        ],
    }
    args.state.write_text(json.dumps(state, indent=2) + "\n")
    print(
        "PASS: cancelled delivery and two queued events; checkpoint saved to",
        args.state,
    )


def verify(args, state):
    delivery = request(args.delivery, f"/api/deliveries/{state['deliveryId']}")
    assert delivery["status"] == "CANCELLED", "Business state did not survive restart"
    for expected in state["events"]:
        row = request(args.delivery, f"/api/messaging/outbox/{expected['eventId']}")
        assert row["publishedAt"] is None, (
            "Keep the relay disabled for the verify phase"
        )
        assert row["aggregateId"] == state["deliveryId"]
        assert (
            row["binding"] == expected["binding"]
            and row["createdAt"] == expected["createdAt"]
        )
    print(
        "PASS: business state, event IDs and queued timestamps survived container recreation"
    )


def drain(args, state):
    deadline = time.monotonic() + args.timeout
    while time.monotonic() < deadline:
        rows = [
            request(args.delivery, f"/api/messaging/outbox/{e['eventId']}")
            for e in state["events"]
        ]
        notes = request(
            args.customer, f"/api/customers/{state['customerId']}/notifications"
        )
        notes = [n for n in notes if n["deliveryId"] == state["deliveryId"]]
        if all(r["publishedAt"] for r in rows) and notes:
            assert len(notes) == 1, "Expected exactly one cancellation notification"
            expected = next(
                e["eventId"]
                for e in state["events"]
                if e["binding"] == "deliveryCancelled-out-0"
            )
            assert notes[0]["sourceEventId"] == expected
            print(
                "PASS: same queued events acknowledged and one cancellation notification persisted"
            )
            return
        time.sleep(1)
    raise SystemExit(
        "Timed out waiting for acknowledged events and notification; inspect outbox status and DLQs"
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("phase", choices=["queue", "verify", "drain"])
    parser.add_argument("--delivery", default="http://localhost:8081")
    parser.add_argument("--customer", default="http://localhost:8084")
    parser.add_argument("--state", type=Path, default=Path("outbox-smoke-state.json"))
    parser.add_argument("--timeout", type=float, default=120)
    args = parser.parse_args()
    if args.phase == "queue":
        queued(args)
    else:
        state = json.loads(args.state.read_text())
        (verify if args.phase == "verify" else drain)(args, state)


if __name__ == "__main__":
    main()
