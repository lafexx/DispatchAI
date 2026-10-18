#!/usr/bin/env python3
"""Populate DispatchAI through its public APIs, without calling the AI service.

Python 3.10+; standard library only. See scripts/README.md for examples.
Each run adds an isolated scenario and journals successful/uncertain writes.
"""

from __future__ import annotations

import argparse
from collections import Counter
from datetime import datetime, timezone
import json
import math
import os
from pathlib import Path
import re
import sys
import time
import urllib.error
import urllib.request
import uuid

PORTS = {
    "delivery": 8081,
    "fleet": 8082,
    "dispatch": 8083,
    "customer": 8084,
    "analytics": 8085,
}
TRANSIENT = {0, 429, 500, 502, 503, 504}
TARGETS = (
    "COMPLETED",
    "COMPLETED",
    "COMPLETED",
    "CANCELLED",
    "PENDING",
    "PENDING",
    "ASSIGNED",
    "IN_TRANSIT",
)
ADDRESSES = (
    "12 King Street, Sydney NSW",
    "85 George Street, Parramatta NSW",
    "2 Wharf Road, Pyrmont NSW",
    "28 Victoria Road, Ryde NSW",
    "145 Pacific Highway, Chatswood NSW",
    "64 Church Street, Parramatta NSW",
    "40 Railway Parade, Burwood NSW",
    "18 Oxford Street, Bondi Junction NSW",
)
NAMES = (
    "Alex Morgan",
    "Jamie Chen",
    "Sam Wilson",
    "Taylor Reed",
    "Jordan Lee",
    "Casey Patel",
    "Riley Davis",
    "Avery Smith",
    "Morgan Evans",
    "Drew Nguyen",
)
COMPANIES = ("Apex Distribution", "Harbour Supply", "Metro Retail", "Eastside Foods")


class SeedError(Exception):
    pass


class ApiError(SeedError):
    def __init__(self, service, method, path, status, detail):
        self.status = status
        super().__init__(
            f"{method} {service}{path}: {status or 'connection error'}: {detail}"
        )


class Api:
    def __init__(self, urls, request_timeout=10):
        self.urls = urls
        self.request_timeout = request_timeout

    def call(self, service, method, path, body=None):
        data = None if body is None else json.dumps(body).encode()
        request = urllib.request.Request(
            self.urls[service] + path,
            data=data,
            method=method,
            headers={"Content-Type": "application/json", "Accept": "application/json"},
        )
        try:
            with urllib.request.urlopen(
                request, timeout=self.request_timeout
            ) as response:
                raw = response.read()
        except urllib.error.HTTPError as error:
            detail = error.read().decode(errors="replace")[:1000]
            raise ApiError(service, method, path, error.code, detail) from error
        except (urllib.error.URLError, TimeoutError, OSError) as error:
            raise ApiError(service, method, path, 0, str(error)) from error
        try:
            return json.loads(raw) if raw else None
        except (ValueError, UnicodeDecodeError) as error:
            raise SeedError(
                f"{method} {service}{path} returned invalid JSON"
            ) from error


def scenario(count):
    plans = [
        {
            "index": i,
            "target": TARGETS[i % len(TARGETS)],
            "delayed": i % len(TARGETS) in {2, 7},
        }
        for i in range(count)
    ]
    # Terminal deliveries free the owned pool before the open work is assigned.
    return sorted(
        plans,
        key=lambda p: (
            0
            if p["target"] in {"COMPLETED", "CANCELLED"}
            else 2
            if p["target"] == "PENDING"
            else 1,
            p["index"],
        ),
    )


class Seeder:
    def __init__(self, api, args, clock=time.monotonic, sleep=time.sleep, log=print):
        self.api, self.args = api, args
        self.clock, self.sleep, self.log = clock, sleep, log
        self.driver_uses = Counter()
        self.state = {
            "version": 1,
            "runId": args.run_id,
            "startedAt": datetime.now(timezone.utc).isoformat(),
            "urls": api.urls,
            "complete": False,
            "customers": [],
            "drivers": [],
            "vehicles": [],
            "deliveries": [],
            "assignments": [],
            "writes": [],
        }

    def save(self):
        path = self.args.output
        temporary = path.with_name(path.name + ".tmp")
        temporary.write_text(json.dumps(self.state, indent=2) + "\n", encoding="utf-8")
        temporary.replace(path)

    def get(self, service, path):
        return self.api.call(service, "GET", path)

    def post(self, service, path, body=None):
        entry = {"service": service, "path": path, "body": body, "status": "requesting"}
        self.state["writes"].append(entry)
        self.save()
        # Never blindly retry a POST: a lost response can hide a committed write.
        try:
            result = self.api.call(service, "POST", path, body)
        except SeedError as error:
            entry.update(status="failed-or-uncertain", error=str(error))
            self.save()
            raise
        entry.update(status="confirmed", response=result)
        self.save()
        return result

    def create(self, collection, service, path, body):
        result = self.post(service, path, body)
        if not isinstance(result, dict) or not result.get("id"):
            raise SeedError(
                f"Creation at {service}{path} did not return an ID; inspect the journal before retrying"
            )
        self.state[collection].append(result)
        self.save()
        return result

    def wait(self, label, read, predicate):
        deadline = self.clock() + self.args.timeout
        last = "condition not yet met"
        while True:
            try:
                value = read()
                if predicate(value):
                    return value
                last = "condition not yet met"
            except ApiError as error:
                if error.status not in TRANSIENT:
                    raise
                last = str(error)
            if self.clock() >= deadline:
                raise SeedError(
                    f"Timed out waiting for {label}: {last}. Check Kafka, service logs and the seed journal."
                )
            self.sleep(min(self.args.poll_interval, max(0, deadline - self.clock())))

    def ready(self):
        probe = "00000000-0000-0000-0000-000000000000"

        def missing(service, path):
            try:
                self.get(service, path)
            except ApiError as error:
                if error.status != 404:
                    raise
            return True

        probes = [
            ("Customer", lambda: self.get("customer", "/api/customers")),
            ("Fleet drivers", lambda: self.get("fleet", "/api/drivers")),
            ("Fleet vehicles", lambda: self.get("fleet", "/api/vehicles")),
            ("Delivery", lambda: missing("delivery", f"/api/deliveries/{probe}")),
            (
                "Dispatch",
                lambda: missing("dispatch", f"/api/dispatch/assignments/{probe}"),
            ),
            ("Analytics", lambda: self.get("analytics", "/api/analytics/deliveries")),
            (
                "Analytics summary",
                lambda: self.get("analytics", "/api/analytics/deliveries/summary"),
            ),
        ]
        for name, read in probes:
            self.wait(name + " readiness", read, lambda _: True)

    def bootstrap(self):
        run = self.args.run_id
        for i in range(self.args.customers):
            self.create(
                "customers",
                "customer",
                "/api/customers",
                {
                    "name": f"{COMPANIES[i % len(COMPANIES)]} · Demo {run}-{i + 1}",
                    "email": f"demo-{run}-{i + 1}@example.com",
                    "phone": "0400000000",
                },
            )
        for i in range(self.args.drivers):
            driver = self.create(
                "drivers",
                "fleet",
                "/api/drivers",
                {"name": f"{NAMES[i % len(NAMES)]} · Demo {run}-{i + 1}"},
            )
            driver.update(
                self.post(
                    "fleet",
                    f"/api/drivers/{driver['id']}/availability",
                    {"available": True},
                )
            )
        for i in range(self.args.vehicles):
            kind, capacity = [("TRUCK", 1500), ("VAN", 500), ("CAR", 100)][i % 3]
            vehicle = self.create(
                "vehicles",
                "fleet",
                "/api/vehicles",
                {
                    "registration": f"DEMO-{run}-{i + 1:02d}".upper(),
                    "type": kind,
                    "capacityKg": capacity,
                },
            )
            vehicle.update(
                self.post(
                    "fleet",
                    f"/api/vehicles/{vehicle['id']}/availability",
                    {"available": True},
                )
            )
        self.log(
            f"Created {self.args.customers} customers, {self.args.drivers} drivers and {self.args.vehicles} vehicles"
        )

    def candidates(self, delivery_id):
        context = self.get(
            "dispatch", f"/api/dispatch/deliveries/{delivery_id}/eligibility"
        )
        owned_drivers = {d["id"] for d in self.state["drivers"]}
        owned_vehicles = {v["id"] for v in self.state["vehicles"]}
        drivers = [d for d in context["availableDrivers"] if d["id"] in owned_drivers]
        vehicles = [v for v in context["eligibleVehicles"] if v["id"] in owned_vehicles]
        if not drivers or not vehicles:
            return None
        driver = min(drivers, key=lambda d: (self.driver_uses[d["id"]], d["id"]))
        vehicle = min(vehicles, key=lambda v: (v["capacityKg"], v["id"]))
        return driver, vehicle

    def populate_delivery(self, plan):
        i, target = plan["index"], plan["target"]
        # Leave manual work light enough for every demo vehicle to carry.
        weight = {"PENDING": 25, "ASSIGNED": 75, "IN_TRANSIT": 250}.get(
            target, [5, 75, 250, 600][i % 4]
        )
        delivery = self.create(
            "deliveries",
            "delivery",
            "/api/deliveries",
            {
                "customerId": self.state["customers"][i % self.args.customers]["id"],
                "pickupAddress": ADDRESSES[i % len(ADDRESSES)],
                "destinationAddress": ADDRESSES[(i + 3) % len(ADDRESSES)],
                "parcelWeightKg": weight,
            },
        )
        delivery.update(targetStatus=target, expectedDelayed=plan["delayed"])
        self.save()
        delivery_id = delivery["id"]
        path = f"/api/deliveries/{delivery_id}"
        if target != "PENDING":
            driver, vehicle = self.wait(
                "an owned eligible pair", lambda: self.candidates(delivery_id), bool
            )
            assignment = self.create(
                "assignments",
                "dispatch",
                "/api/dispatch/assignments",
                {
                    "deliveryId": delivery_id,
                    "driverId": driver["id"],
                    "vehicleId": vehicle["id"],
                },
            )
            self.driver_uses[driver["id"]] += 1
            delivery.update(assignedDriverId=driver["id"], vehicleId=vehicle["id"])
            self.save()

            def accepted():
                state = self.get(
                    "dispatch", f"/api/dispatch/assignments/{assignment['id']}"
                )
                if state["status"] == "REJECTED":
                    raise SeedError(
                        f"Assignment {assignment['id']} was rejected; inspect services before a new seed run"
                    )
                return self.get("delivery", path)

            self.wait(
                "Delivery to confirm assignment",
                accepted,
                lambda d: (
                    d["status"] == "ASSIGNED"
                    and d.get("assignedDriverId") == driver["id"]
                ),
            )
            if target == "CANCELLED":
                self.post(
                    "delivery",
                    path + "/cancel",
                    {
                        "reason": "CUSTOMER_REQUEST",
                        "notes": "Demo scenario: customer changed plans",
                    },
                )
            elif target in {"IN_TRANSIT", "COMPLETED"}:
                self.post("delivery", path + "/start")
                if plan["delayed"]:
                    self.post(
                        "delivery",
                        path + "/delay",
                        {
                            "delayMinutes": [10, 15, 25][i % 3],
                            "reason": "Demo scenario: heavy traffic",
                        },
                    )
                if target == "COMPLETED":
                    self.post("delivery", path + "/complete")
            delivery.update(
                self.wait(
                    f"delivery state {target}",
                    lambda: self.get("delivery", path),
                    lambda d: d["status"] == target,
                )
            )
            if target in {"COMPLETED", "CANCELLED"}:
                self.wait(
                    "Dispatch terminal state",
                    lambda: self.get(
                        "dispatch", f"/api/dispatch/assignments/{assignment['id']}"
                    ),
                    lambda a: a["status"] == target,
                )
                self.wait(
                    "driver release",
                    lambda: self.get("fleet", f"/api/drivers/{driver['id']}"),
                    lambda d: d["availability"] == "AVAILABLE",
                )
                self.wait(
                    "vehicle release",
                    lambda: self.get("fleet", f"/api/vehicles/{vehicle['id']}"),
                    lambda v: v["available"] is True,
                )
        self.log(
            f"[{len(self.state['deliveries']):02d}/{self.args.deliveries}] {target:10} {'delayed' if plan['delayed'] else '       '} {delivery_id}"
        )

    def finish(self):
        expected = {d["id"]: d for d in self.state["deliveries"]}

        def projected(rows):
            actual = {d["deliveryId"]: d for d in rows}
            return all(
                did in actual
                and actual[did]["status"] == d["targetStatus"]
                and bool(actual[did]["delayed"]) == d["expectedDelayed"]
                and (
                    not d.get("assignedDriverId")
                    or actual[did].get("driverId") == d["assignedDriverId"]
                )
                for did, d in expected.items()
            )

        self.wait(
            "all demo analytics projections",
            lambda: self.get("analytics", "/api/analytics/deliveries"),
            projected,
        )
        pending = [d["id"] for d in expected.values() if d["targetStatus"] == "PENDING"]
        # Verify that the demo leaves usable resources instead of only busy ones.
        drivers = self.get("fleet", "/api/drivers/available")
        vehicles = self.get("fleet", "/api/vehicles")
        owned_drivers = {d["id"] for d in self.state["drivers"]}
        owned_vehicles = {v["id"] for v in self.state["vehicles"]}
        ready_drivers = [d["id"] for d in drivers if d["id"] in owned_drivers]
        ready_vehicles = [
            v["id"] for v in vehicles if v["id"] in owned_vehicles and v["available"]
        ]
        if not ready_drivers or not ready_vehicles:
            raise SeedError(
                "No spare owned resources remain; inspect the journal and Fleet before demoing"
            )
        for delivery_id in pending:
            if not self.candidates(delivery_id):
                raise SeedError(
                    f"Pending delivery {delivery_id} has no owned eligible pair"
                )
        self.state.update(
            complete=True,
            pendingDeliveryIds=pending,
            availableDriverIds=ready_drivers,
            availableVehicleIds=ready_vehicles,
            outcomeCounts=dict(Counter(d["targetStatus"] for d in expected.values())),
            completedAt=datetime.now(timezone.utc).isoformat(),
        )
        self.save()
        self.log("\nDemo ready: " + json.dumps(self.state["outcomeCounts"]))
        self.log(
            f"Spare drivers: {len(ready_drivers)}; spare vehicles: {len(ready_vehicles)}"
        )
        self.log("IDs and request journal: " + str(self.args.output))

    def run(self):
        self.ready()  # Probe every required service before the first write.
        self.save()
        self.bootstrap()
        plans = scenario(self.args.deliveries)
        for index, plan in enumerate(plans):
            self.populate_delivery(plan)
            if index < len(plans) - 1 and self.args.interval:
                self.sleep(self.args.interval)
        self.finish()


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--customers", type=int, default=4)
    parser.add_argument("--drivers", type=int, default=12)
    parser.add_argument("--vehicles", type=int, default=12)
    parser.add_argument("--deliveries", type=int, default=24)
    parser.add_argument(
        "--timeout",
        type=float,
        default=90,
        help="Maximum seconds for each asynchronous/readiness wait",
    )
    parser.add_argument("--request-timeout", type=float, default=10)
    parser.add_argument("--poll-interval", type=float, default=0.5)
    parser.add_argument(
        "--interval",
        type=float,
        default=1,
        help="Seconds between deliveries; increase to spread real chart activity",
    )
    parser.add_argument("--run-id", default=uuid.uuid4().hex[:8])
    parser.add_argument(
        "--output",
        type=Path,
        help="New JSON journal path (existing files are never overwritten)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print the scenario without contacting services or writing files",
    )
    for name, port in PORTS.items():
        parser.add_argument(
            f"--{name}-url",
            default=os.environ.get(f"{name.upper()}_URL", f"http://localhost:{port}"),
        )
    args = parser.parse_args(argv)
    if min(args.customers, args.drivers, args.vehicles) < 1 or args.deliveries < 0:
        parser.error(
            "customers/drivers/vehicles must be positive; deliveries may be zero"
        )
    if (
        any(
            not math.isfinite(v) or v <= 0
            for v in [args.timeout, args.request_timeout, args.poll_interval]
        )
        or not math.isfinite(args.interval)
        or args.interval < 0
    ):
        parser.error(
            "timeouts and poll interval must be finite and positive; interval must be finite and nonnegative"
        )
    if not re.fullmatch(r"[A-Za-z0-9-]{1,24}", args.run_id):
        parser.error("run-id must contain 1–24 letters, digits or hyphens")
    active = sum(
        p["target"] in {"ASSIGNED", "IN_TRANSIT"} for p in scenario(args.deliveries)
    )
    if min(args.drivers, args.vehicles) < active + 1:
        parser.error(
            f"this scenario leaves {active} active assignments; supply at least {active + 1} drivers AND vehicles to retain a spare pair"
        )
    args.output = args.output or Path(f"demo-seed-{args.run_id}.json")
    if (
        args.output.exists()
        or args.output.with_name(args.output.name + ".tmp").exists()
    ):
        parser.error(
            "output/journal already exists; choose a new output path and run ID"
        )
    if not args.output.parent.is_dir():
        parser.error("output parent directory does not exist")
    for name in PORTS:
        url = getattr(args, name + "_url").rstrip("/")
        if not url.startswith(("http://", "https://")):
            parser.error(f"{name}-url must be an HTTP(S) URL")
        setattr(args, name + "_url", url)
    return args


def main(argv=None):
    args = parse_args(argv)
    urls = {name: getattr(args, name + "_url") for name in PORTS}
    if args.dry_run:
        print(
            json.dumps(
                {
                    "runId": args.run_id,
                    "urls": urls,
                    "customers": args.customers,
                    "drivers": args.drivers,
                    "vehicles": args.vehicles,
                    "deliveries": args.deliveries,
                    "outcomes": dict(
                        Counter(p["target"] for p in scenario(args.deliveries))
                    ),
                    "journal": str(args.output),
                },
                indent=2,
            )
        )
        return 0
    seeder = Seeder(Api(urls, args.request_timeout), args)
    try:
        seeder.run()
    except (
        SeedError,
        OSError,
        KeyError,
        TypeError,
        ValueError,
        KeyboardInterrupt,
    ) as error:
        print(f"\nSeeding stopped: {error or 'interrupted'}", file=sys.stderr)
        print(
            f"Existing data is retained. Inspect {args.output} before retrying; POST requests are not automatically retried.",
            file=sys.stderr,
        )
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
