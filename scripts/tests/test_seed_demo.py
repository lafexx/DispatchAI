"""Contract-shaped API tests; no running services or model are contacted."""

import importlib.util
import io
import json
import tempfile
import unittest
import urllib.request
from pathlib import Path
from typing import Any
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location(
    "seed_demo", Path(__file__).resolve().parents[1] / "seed-demo.py"
)
if SPEC is None or SPEC.loader is None:
    raise ImportError("Could not load the demo seeder for testing")
seed = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(seed)

# The fake API stores heterogeneous JSON records, just like decoded responses.
# Keep their container type explicit rather than inferring every value as str.
Record = dict[str, Any]


class Clock:
    def __init__(self):
        self.value = 0

    def now(self):
        return self.value

    def sleep(self, seconds):
        self.value += seconds


class FakeApi:
    """Enforce the service contracts and defer assignment/projection/release."""

    urls = {name: "http://" + name for name in seed.PORTS}

    def __init__(self):
        self.calls: list[tuple[str, str, str, Record | None]] = []
        self.customers: dict[str, Record] = {}
        self.deliveries: dict[str, Record] = {}
        self.assignments: dict[str, Record] = {}
        self.drivers: dict[str, Record] = {
            "external-driver": {"id": "external-driver", "availability": "AVAILABLE"}
        }
        self.vehicles: dict[str, Record] = {
            "external-vehicle": {
                "id": "external-vehicle",
                "available": True,
                "capacityKg": 9999,
            }
        }
        self.acceptance: dict[str, int] = {}
        self.release: dict[str, int] = {}
        self.analytics_reads = 0
        self.reject = False
        self.offline = False
        self.uncertain = False

    def call(
        self, service: str, method: str, path: str, body: Record | None = None
    ) -> Record | list[Record]:
        self.calls.append((service, method, path, body))
        assert service in seed.PORTS, "No AI calls"
        if self.offline:
            raise seed.ApiError(service, method, path, 503, "offline")
        if self.uncertain and method == "POST":
            raise seed.ApiError(service, method, path, 0, "response lost")
        parts = path.strip("/").split("/")
        if method == "POST" and path in {
            "/api/customers",
            "/api/drivers",
            "/api/vehicles",
            "/api/deliveries",
        }:
            assert body is not None, "Creation requires a JSON body"
            table = {
                "customers": self.customers,
                "drivers": self.drivers,
                "vehicles": self.vehicles,
                "deliveries": self.deliveries,
            }[parts[-1]]
            item: Record = dict(body, id=f"{parts[-1]}-{len(table)}")
            if parts[-1] == "drivers":
                item["availability"] = "UNAVAILABLE"
            if parts[-1] == "vehicles":
                assert body["capacityKg"] > 0 and body["type"] in {
                    "CAR",
                    "VAN",
                    "TRUCK",
                }
                item["available"] = False
            if parts[-1] == "deliveries":
                assert (
                    body["customerId"] in self.customers and body["parcelWeightKg"] > 0
                )
                item.update(status="PENDING", delayed=False, assignedDriverId=None)
            table[item["id"]] = item
            return dict(item)
        if service == "customer":
            return list(self.customers.values())
        if service == "fleet":
            table = self.drivers if parts[1] == "drivers" else self.vehicles
            if method == "POST":
                assert parts[-1] == "availability" and parts[-2] in table
                assert body == {"available": True}
                table[parts[-2]][
                    "availability" if table is self.drivers else "available"
                ] = "AVAILABLE" if table is self.drivers else True
                return dict(table[parts[-2]])
            if parts[-1] == "available":
                return [
                    dict(d) for d in table.values() if d["availability"] == "AVAILABLE"
                ]
            if len(parts) == 3:
                return dict(table[parts[-1]])
            return [dict(x) for x in table.values()]
        if service == "delivery":
            if parts[2] not in self.deliveries:
                raise seed.ApiError(service, method, path, 404, "missing")
            d = self.deliveries[parts[2]]
            if method == "GET":
                if d["id"] in self.acceptance:
                    self.acceptance[d["id"]] -= 1
                    if self.acceptance[d["id"]] <= 0:
                        a = next(
                            a
                            for a in self.assignments.values()
                            if a["deliveryId"] == d["id"]
                        )
                        d.update(status="ASSIGNED", assignedDriverId=a["driverId"])
                        del self.acceptance[d["id"]]
                return dict(d)
            action = parts[-1]
            if action == "start":
                assert d["status"] == "ASSIGNED"
                assert body is None
                d["status"] = "IN_TRANSIT"
            elif action == "delay":
                assert body is not None, "Delay requires a JSON body"
                assert d["status"] == "IN_TRANSIT" and body["delayMinutes"] > 0
                d["delayed"] = True
            elif action in {"complete", "cancel"}:
                assert d["status"] == (
                    "IN_TRANSIT" if action == "complete" else "ASSIGNED"
                )
                if action == "cancel":
                    assert body is not None, "Cancellation requires a JSON body"
                    assert body["reason"] == "CUSTOMER_REQUEST"
                d["status"] = "COMPLETED" if action == "complete" else "CANCELLED"
                a = next(
                    a for a in self.assignments.values() if a["deliveryId"] == d["id"]
                )
                self.release[a["id"]] = 2
            else:
                raise AssertionError(path)
            return dict(d)
        if service == "dispatch":
            if path.endswith("/eligibility"):
                d = self.deliveries[parts[-2]]
                return {
                    "availableDrivers": [
                        dict(d)
                        for d in self.drivers.values()
                        if d["availability"] == "AVAILABLE"
                    ],
                    "eligibleVehicles": [
                        dict(v)
                        for v in self.vehicles.values()
                        if v["available"] and v["capacityKg"] >= d["parcelWeightKg"]
                    ],
                }
            if method == "POST":
                assert body is not None, "Assignment requires a JSON body"
                assert (
                    path == "/api/dispatch/assignments"
                    and body["vehicleId"] is not None
                )
                assert not body["driverId"].startswith("external") and not body[
                    "vehicleId"
                ].startswith("external")
                d, driver, vehicle = (
                    self.deliveries[body["deliveryId"]],
                    self.drivers[body["driverId"]],
                    self.vehicles[body["vehicleId"]],
                )
                assert (
                    d["status"] == "PENDING"
                    and driver["availability"] == "AVAILABLE"
                    and vehicle["available"]
                )
                assert vehicle["capacityKg"] >= d["parcelWeightKg"]
                a = dict(
                    body,
                    id=f"assignment-{len(self.assignments)}",
                    status="REJECTED" if self.reject else "ACTIVE",
                )
                self.assignments[a["id"]] = a
                driver["availability"] = "UNAVAILABLE"
                vehicle["available"] = False
                self.acceptance[d["id"]] = 2
                return dict(a)
            if parts[-1] not in self.assignments:
                raise seed.ApiError(service, method, path, 404, "missing")
            a = self.assignments[parts[-1]]
            if a["id"] in self.release:
                self.release[a["id"]] -= 1
                if self.release[a["id"]] <= 0:
                    a["status"] = self.deliveries[a["deliveryId"]]["status"]
                    self.drivers[a["driverId"]]["availability"] = "AVAILABLE"
                    self.vehicles[a["vehicleId"]]["available"] = True
                    del self.release[a["id"]]
            return dict(a)
        if service == "analytics":
            if path.endswith("/summary"):
                return {"total": len(self.deliveries)}
            self.analytics_reads += 1
            if self.analytics_reads < 4:
                return []
            return [
                dict(
                    deliveryId=d["id"],
                    status=d["status"],
                    delayed=d["delayed"],
                    driverId=d["assignedDriverId"],
                )
                for d in self.deliveries.values()
            ]
        raise AssertionError((service, method, path, body))


class SeedTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.path = Path(self.directory.name) / "seed.json"

    def args(self, *extra):
        return seed.parse_args(
            ["--output", str(self.path), "--interval", "0", "--timeout", "3", *extra]
        )

    def runner(self, api, *extra):
        clock = Clock()
        return seed.Seeder(
            api, self.args(*extra), clock.now, clock.sleep, lambda _: None
        )

    def test_full_demo_uses_owned_capacity_matching_and_waits_for_events(self):
        api = FakeApi()
        run = self.runner(api)
        run.run()
        state = json.loads(self.path.read_text())
        self.assertTrue(state["complete"])
        self.assertEqual(
            state["outcomeCounts"],
            {
                "COMPLETED": 9,
                "CANCELLED": 3,
                "ASSIGNED": 3,
                "IN_TRANSIT": 3,
                "PENDING": 6,
            },
        )
        self.assertEqual(len(state["availableDriverIds"]), 6)
        self.assertEqual(len(state["availableVehicleIds"]), 6)
        self.assertEqual(len(state["assignments"]), 18)
        self.assertEqual(sum(d["delayed"] for d in api.deliveries.values()), 6)
        self.assertFalse(api.release)
        self.assertFalse(api.acceptance)
        self.assertTrue(all(w["status"] == "confirmed" for w in state["writes"]))
        self.assertEqual(api.drivers["external-driver"]["availability"], "AVAILABLE")
        self.assertTrue(api.vehicles["external-vehicle"]["available"])

    def test_minimum_pool_still_leaves_eligible_spares(self):
        api = FakeApi()
        run = self.runner(api, "--drivers", "7", "--vehicles", "7")
        run.run()
        self.assertEqual(len(run.state["availableVehicleIds"]), 1)
        self.assertEqual(len(run.state["availableDriverIds"]), 1)

    def test_fleet_only_run(self):
        run = self.runner(
            FakeApi(), "--deliveries", "0", "--drivers", "1", "--vehicles", "1"
        )
        run.run()
        self.assertTrue(run.state["complete"])
        self.assertEqual(run.state["deliveries"], [])

    def test_rejected_assignment_is_not_retried_or_started(self):
        api = FakeApi()
        api.reject = True
        with self.assertRaisesRegex(seed.SeedError, "rejected"):
            self.runner(api).run()
        self.assertEqual(len(api.assignments), 1)
        self.assertFalse(any(path.endswith("/start") for _, _, path, _ in api.calls))
        self.assertFalse(json.loads(self.path.read_text())["complete"])

    def test_readiness_failure_writes_no_records(self):
        api = FakeApi()
        api.offline = True
        with self.assertRaisesRegex(seed.SeedError, "Timed out"):
            self.runner(api).run()
        self.assertFalse(any(method == "POST" for _, method, _, _ in api.calls))
        self.assertFalse(self.path.exists())

    def test_uncertain_post_is_journaled_and_not_retried(self):
        api = FakeApi()
        api.uncertain = True
        with self.assertRaisesRegex(seed.ApiError, "response lost"):
            self.runner(api).run()
        self.assertEqual(sum(m == "POST" for _, m, _, _ in api.calls), 1)
        state = json.loads(self.path.read_text())
        self.assertEqual(state["writes"][0]["status"], "failed-or-uncertain")
        self.assertFalse(state["complete"])

    def test_invalid_sizes_and_existing_output_are_refused(self):
        for extra in [
            ("--vehicles", "6"),
            ("--deliveries", "-1"),
            ("--timeout", "nan"),
            ("--interval", "inf"),
        ]:
            with (
                self.subTest(extra=extra),
                patch("sys.stderr", new_callable=io.StringIO),
                self.assertRaises(SystemExit),
            ):
                self.args(*extra)
        self.path.write_text("keep")
        with (
            patch("sys.stderr", new_callable=io.StringIO),
            self.assertRaises(SystemExit),
        ):
            self.args()
        self.assertEqual(self.path.read_text(), "keep")

    def test_dry_run_is_offline_and_does_not_write(self):
        with (
            patch.object(
                seed.Api, "call", side_effect=AssertionError("Unexpected request")
            ),
            patch("sys.stdout", new_callable=io.StringIO),
        ):
            self.assertEqual(seed.main(["--dry-run", "--output", str(self.path)]), 0)
        self.assertFalse(self.path.exists())

    def test_http_request_keeps_gateway_prefix_and_json_body(self):
        class Response:
            def __enter__(self):
                return self

            def __exit__(self, *_):
                pass

            def read(self):
                return b'{"id":"vehicle-1"}'

        api = seed.Api({"fleet": "http://gateway/fleet-api"})
        with patch.object(
            seed.urllib.request, "urlopen", return_value=Response()
        ) as open_url:
            api.call("fleet", "POST", "/api/vehicles", {"capacityKg": 500})
        call_args = open_url.call_args
        assert call_args is not None, "Expected an HTTP request"
        req = call_args.args[0]
        assert isinstance(req, urllib.request.Request)
        self.assertEqual(req.full_url, "http://gateway/fleet-api/api/vehicles")
        self.assertEqual(req.method, "POST")
        data = req.data
        assert isinstance(data, bytes), "Expected an encoded JSON request body"
        self.assertEqual(json.loads(data), {"capacityKg": 500})


if __name__ == "__main__":
    unittest.main()
