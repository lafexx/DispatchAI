"""Contract-shaped mock APIs: no Spring build or running DispatchAI stack required."""
import importlib.util
import json
import tempfile
import threading
import unittest
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

SPEC = importlib.util.spec_from_file_location("simulate", Path(__file__).parents[1] / "simulate.py")
mod = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = mod
SPEC.loader.exec_module(mod)


class MockApi:
    def __init__(self):
        self.customers, self.drivers, self.deliveries, self.assignments = {}, {}, {}, {}
        self.calls = []
        self.uncertain_assignment = False
        self.delay_propagation = False

    def get(self, service, path):
        if path == "/api/customers":
            return list(self.customers.values())
        if path == "/api/drivers":
            return list(self.drivers.values())
        if path == "/api/drivers/available":
            return [d for d in self.drivers.values() if d["availability"] == "AVAILABLE"]
        if service == "dispatch" and path.endswith("/assignments"):
            driver = path.split("/")[-2]
            return [a for a in self.assignments.values() if a["driverId"] == driver]
        if service == "dispatch" and path.endswith("/assignment"):
            delivery = path.split("/")[-2]
            if delivery in self.assignments:
                return self.assignments[delivery]
            raise mod.ApiError(404, "GET assignment: not found")
        collection = {"customer": self.customers, "fleet": self.drivers,
                      "delivery": self.deliveries, "dispatch": {}}[service]
        if path.split("/")[-1] not in collection:
            raise mod.ApiError(404, "GET entity: not found")
        return collection[path.split("/")[-1]].copy()

    def post(self, service, path, body=None):
        self.calls.append((service, path, body))
        if path == "/api/customers":
            value = dict(body, id=f"customer-{len(self.customers)}")
            self.customers[value["id"]] = value
            return value
        if path == "/api/drivers":
            value = dict(body, id=f"driver-{len(self.drivers)}", availability="UNAVAILABLE")
            self.drivers[value["id"]] = value
            return value
        if path.endswith("/availability"):
            driver = self.drivers[path.split("/")[-2]]
            driver["availability"] = "AVAILABLE" if body["available"] else "UNAVAILABLE"
            return driver
        if path == "/api/deliveries":
            value = dict(body, id=f"delivery-{len(self.deliveries)}", status="PENDING", assignedDriverId=None)
            self.deliveries[value["id"]] = value
            return value
        if path == "/api/dispatch/assignments":
            self.assignments[body["deliveryId"]] = dict(body, status="ACTIVE")
            if not self.delay_propagation:
                self.propagate(body["deliveryId"])
            if self.uncertain_assignment:
                raise mod.ApiError(None, "POST assignment timed out after persistence")
            return self.assignments[body["deliveryId"]]
        delivery = self.deliveries[path.split("/")[-2]]
        action = path.split("/")[-1]
        if action == "start":
            assert delivery["status"] == "ASSIGNED"
            delivery["status"] = "IN_TRANSIT"
        elif action == "complete":
            assert delivery["status"] == "IN_TRANSIT"
            delivery["status"] = "COMPLETED"
        elif action == "cancel":
            assert delivery["status"] not in mod.TERMINAL
            delivery["status"] = "CANCELLED"
        elif action == "delay":
            assert delivery["status"] == "IN_TRANSIT"
            delivery["delayed"] = True
        else:
            raise AssertionError(path)
        return delivery.copy()

    def propagate(self, delivery_id, fleet=True):
        assignment = self.assignments[delivery_id]
        self.deliveries[delivery_id].update(status="ASSIGNED", assignedDriverId=assignment["driverId"])
        if fleet:
            self.drivers[assignment["driverId"]]["availability"] = "UNAVAILABLE"


class SimulatorTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.now = 1000
        self.api = MockApi()
        self.config = mod.Config(state_file=str(Path(self.tmp.name) / "state.json"),
                                 pending_target=0, drivers=3, initial=0)
        self.sim = mod.Simulator(self.config, self.api, lambda: self.now)
        self.sim.bootstrap()

    def job(self, outcome="complete"):
        self.sim.arrive()
        job = list(self.sim.jobs.values())[-1]
        job.outcome = outcome
        job.due = self.now
        return job

    def test_complete_lifecycle_recycles_owned_driver(self):
        job = self.job()
        for _ in range(12):
            if job.delivery_id in self.sim.jobs:
                self.sim.advance(job)
            self.now += 20
        self.assertEqual(self.api.deliveries[job.delivery_id]["status"], "COMPLETED")
        self.assertNotIn(job.delivery_id, self.sim.jobs)
        self.assertEqual(self.api.drivers[job.driver_id]["availability"], "AVAILABLE")

    def test_assignment_waits_for_delivery_and_fleet(self):
        job = self.job()
        self.api.delay_propagation = True
        self.sim.advance(job)
        self.now += 100
        self.sim.advance(job)
        self.assertEqual(self.api.deliveries[job.delivery_id]["status"], "PENDING")
        self.api.propagate(job.delivery_id, fleet=False)
        self.sim.advance(job)
        self.assertEqual(self.api.deliveries[job.delivery_id]["status"], "ASSIGNED")
        self.api.drivers[job.driver_id]["availability"] = "UNAVAILABLE"
        self.sim.advance(job)
        self.now += 10
        self.sim.advance(job)
        self.assertEqual(self.api.deliveries[job.delivery_id]["status"], "IN_TRANSIT")

    def test_ambiguous_assignment_is_not_reposted(self):
        job = self.job()
        self.api.delay_propagation = True
        self.api.uncertain_assignment = True
        with self.assertRaises(mod.ApiError):
            self.sim.advance(job)
        self.now += 100
        for _ in range(3):
            self.sim.advance(job)
        calls = [c for c in self.api.calls if c[1] == "/api/dispatch/assignments"]
        self.assertEqual(len(calls), 1)
        self.assertNotIn(job.driver_id, self.sim.available())
        self.api.propagate(job.delivery_id)
        self.sim.advance(job)
        self.now += 10
        self.sim.advance(job)
        self.assertEqual(self.api.deliveries[job.delivery_id]["status"], "IN_TRANSIT")

    def test_pending_human_workflow_is_never_advanced(self):
        self.config.pending_target = 1
        job = self.job()
        self.now += 1000
        self.sim.advance(job)
        self.assertFalse(job.automated)
        self.assertEqual(self.api.deliveries[job.delivery_id]["status"], "PENDING")
        self.api.post("dispatch", "/api/dispatch/assignments", {
            "deliveryId": job.delivery_id, "driverId": self.sim.driver_ids[0]})
        self.sim.advance(job)
        self.assertEqual(self.api.deliveries[job.delivery_id]["status"], "ASSIGNED")

    def test_delay_happens_once_before_completion(self):
        job = self.job("delay")
        for _ in range(20):
            if job.delivery_id in self.sim.jobs:
                self.sim.advance(job)
            self.now += 10
        calls = [c for c in self.api.calls if c[1].endswith("/delay")]
        self.assertEqual(len(calls), 1)
        self.assertEqual(self.api.deliveries[job.delivery_id]["status"], "COMPLETED")

    def test_cancel_from_assigned(self):
        job = self.job("cancel")
        for _ in range(8):
            if job.delivery_id in self.sim.jobs:
                self.sim.advance(job)
            self.now += 10
        self.assertEqual(self.api.deliveries[job.delivery_id]["status"], "CANCELLED")
        self.assertFalse(any(c[1].endswith("/start") for c in self.api.calls))

    def test_resume_preserves_ids_and_reservations(self):
        job = self.job()
        self.api.delay_propagation = True
        self.sim.advance(job)
        self.sim.save()
        resumed = mod.Simulator(self.config, self.api, lambda: self.now)
        resumed.bootstrap()
        self.assertEqual(resumed.customer_id, self.sim.customer_id)
        self.assertEqual(resumed.driver_ids, self.sim.driver_ids)
        self.assertTrue(resumed.jobs[job.delivery_id].assignment_attempted)
        self.assertEqual(len(self.api.customers), 1)
        self.assertEqual(len(self.api.drivers), 3)

    def test_other_drivers_are_not_selected(self):
        foreign = self.api.post("fleet", "/api/drivers", {"name": "Human driver"})
        self.api.post("fleet", f"/api/drivers/{foreign['id']}/availability", {"available": True})
        self.assertNotIn(foreign["id"], self.sim.available())

    def test_release_checks_untracked_assignments(self):
        job = self.job()
        self.sim.advance(job)
        self.sim.advance(job)
        self.api.deliveries[job.delivery_id]["status"] = "COMPLETED"
        external = self.api.post("delivery", "/api/deliveries", {"customerId": self.sim.customer_id})
        self.api.post("dispatch", "/api/dispatch/assignments", {
            "deliveryId": external["id"], "driverId": job.driver_id})
        self.sim.advance(job)
        self.assertIn(job.delivery_id, self.sim.jobs)
        self.assertEqual(self.api.drivers[job.driver_id]["availability"], "UNAVAILABLE")

    def test_recycling_can_be_disabled(self):
        job = self.job()
        self.sim.advance(job)
        self.api.deliveries[job.delivery_id]["status"] = "COMPLETED"
        self.config.recycle = False
        self.sim.advance(job)
        self.assertEqual(self.api.drivers[job.driver_id]["availability"], "UNAVAILABLE")

    def test_caps_prevent_unbounded_arrivals(self):
        self.config.max_deliveries = 1
        self.job()
        self.sim.arrive()
        self.assertEqual(self.sim.created, 1)

    def test_missing_saved_customer_requires_explicit_reset(self):
        self.sim.save()
        self.api.customers.clear()
        with self.assertRaisesRegex(ValueError, "Saved customer"):
            self.sim.bootstrap()

    def test_continuous_activity_keeps_pending_queue_and_reuses_pool(self):
        self.config.pending_target = 2
        self.config.max_deliveries = 18
        for _ in range(500):
            self.sim.tick()
            self.now += 2
            active = sum(j.automated and j.status not in mod.TERMINAL for j in self.sim.jobs.values())
            self.assertLessEqual(active, self.config.max_active)
        pending = [d for d in self.api.deliveries.values() if d["status"] == "PENDING"]
        terminal = [d for d in self.api.deliveries.values() if d["status"] in mod.TERMINAL]
        self.assertEqual(self.sim.created, 18)
        self.assertEqual(len(pending), 2)
        self.assertEqual(len(terminal), 16)
        self.assertEqual(len(self.api.drivers), 3)


class HttpTests(unittest.TestCase):
    def test_json_transport_and_http_error_status(self):
        seen = []
        class Handler(BaseHTTPRequestHandler):
            def log_message(self, *_):
                pass
            def do_POST(self):
                seen.append(json.loads(self.rfile.read(int(self.headers["Content-Length"]))))
                self.send_response(201)
                self.end_headers()
                self.wfile.write(b'{"id":"created"}')
            def do_GET(self):
                self.send_response(404)
                self.end_headers()
                self.wfile.write(b'{"message":"not found"}')
        server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            api = mod.Api()
            api.urls["delivery"] = f"http://127.0.0.1:{server.server_port}"
            result = api.post("delivery", "/api/deliveries", {"customerId": "x"})
            self.assertEqual(result["id"], "created")
            self.assertEqual(seen, [{"customerId": "x"}])
            with self.assertRaises(mod.ApiError) as raised:
                api.get("delivery", "/api/deliveries/missing")
            self.assertEqual(raised.exception.status, 404)
        finally:
            server.shutdown()
            server.server_close()
            thread.join()


if __name__ == "__main__":
    unittest.main()
