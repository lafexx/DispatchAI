#!/usr/bin/env python3
"""DispatchAI demo traffic. Python standard library only; no LLM calls."""
import json
import logging
import math
import os
import random
import signal
import threading
import time
import uuid
from dataclasses import asdict, dataclass
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

LOG = logging.getLogger("simulator")
TERMINAL = {"COMPLETED", "CANCELLED"}
ADDRESSES = ["12 King St, Sydney", "45 George St, Sydney", "8 Pitt St, Sydney",
             "90 Clarence St, Sydney", "22 York St, Sydney", "5 Harbour St, Sydney"]


def number(name, default, cast=float, minimum=0):
    value = cast(os.getenv(name, str(default)))
    if not math.isfinite(value) or value < minimum:
        raise ValueError(f"{name} must be finite and >= {minimum}")
    return value


@dataclass
class Config:
    tick: float = 1
    arrival: float = 10
    transit: float = 45
    speed: float = 1
    drivers: int = 12
    initial: int = 6
    pending_target: int = 3
    max_active: int = 6
    max_tracked: int = 24
    max_deliveries: int = 120
    delay_probability: float = .25
    cancel_probability: float = .15
    recycle: bool = True
    state_file: str = "simulator-state.json"
    seed: int = 318

    @classmethod
    def from_env(cls):
        c = cls(
            tick=number("SIM_TICK_SECONDS", 1, minimum=.1),
            arrival=number("SIM_ARRIVAL_SECONDS", 10, minimum=1),
            transit=number("SIM_TRANSIT_SECONDS", 45, minimum=5),
            speed=number("SIM_SPEED", 1, minimum=.1),
            drivers=number("SIM_DRIVERS", 12, int, 1),
            initial=number("SIM_INITIAL_DELIVERIES", 6, int),
            pending_target=number("SIM_PENDING_TARGET", 3, int),
            max_active=number("SIM_MAX_ACTIVE", 6, int, 1),
            max_tracked=number("SIM_MAX_TRACKED", 24, int, 1),
            max_deliveries=number("SIM_MAX_DELIVERIES", 120, int, 1),
            delay_probability=number("SIM_DELAY_PROBABILITY", .25),
            cancel_probability=number("SIM_CANCEL_PROBABILITY", .15),
            recycle=os.getenv("SIM_RECYCLE_DRIVERS", "true").lower() == "true",
            state_file=os.getenv("SIM_STATE_FILE", "simulator-state.json"),
            seed=number("SIM_SEED", 318, int),
        )
        if c.delay_probability + c.cancel_probability > 1:
            raise ValueError("Delay and cancellation probabilities must sum to <= 1")
        if c.pending_target > c.max_tracked or c.initial > c.max_deliveries:
            raise ValueError("Pending target/initial batch exceeds its configured cap")
        return c


class ApiError(Exception):
    def __init__(self, status, message):
        self.status = status
        super().__init__(message)


class Api:
    def __init__(self):
        self.urls = {name: os.getenv(f"{name.upper()}_URL", f"http://localhost:{port}").rstrip("/")
                     for name, port in [("delivery", 8081), ("fleet", 8082),
                                        ("dispatch", 8083), ("customer", 8084)]}
        self.timeout = number("SIM_HTTP_TIMEOUT_SECONDS", 3, minimum=.1)

    def request(self, service, method, path, body=None):
        data = json.dumps(body).encode() if body is not None else None
        request = Request(self.urls[service] + path, data=data, method=method,
                          headers={"Accept": "application/json", "Content-Type": "application/json"})
        try:
            with urlopen(request, timeout=self.timeout) as response:
                raw = response.read()
                return json.loads(raw) if raw else None
        except HTTPError as exc:
            detail = exc.read().decode(errors="replace")[:400]
            raise ApiError(exc.code, f"{method} {service}{path}: HTTP {exc.code} {detail}") from exc
        except (URLError, OSError, ValueError) as exc:
            raise ApiError(None, f"{method} {service}{path}: {exc}") from exc

    def get(self, service, path):
        return self.request(service, "GET", path)

    def post(self, service, path, body=None):
        return self.request(service, "POST", path, body)


@dataclass
class Job:
    delivery_id: str
    automated: bool
    outcome: str
    due: float
    status: str = "PENDING"
    driver_id: str | None = None
    assignment_attempted: bool = False
    fleet_observed: bool = False
    delay_attempted: bool = False
    complete_due: float = 0
    warned: bool = False


class Simulator:
    def __init__(self, config, api, clock=time.time):
        self.c, self.api, self.clock = config, api, clock
        self.rng = random.Random(config.seed)
        self.path = Path(config.state_file)
        self.session = uuid.uuid4().hex[:8]
        self.customer_id = None
        self.driver_ids = []
        self.jobs = {}
        self.created = 0
        self.next_arrival = 0
        self.initial_remaining = config.initial
        if self.path.exists():
            state = json.loads(self.path.read_text())
            if state.get("version") != 1:
                raise ValueError("Unsupported simulator state version")
            self.session = state["session"]
            self.customer_id = state["customer_id"]
            self.driver_ids = state["driver_ids"]
            self.jobs = {j["delivery_id"]: Job(**j) for j in state["jobs"]}
            self.created = state["created"]
            self.next_arrival = state["next_arrival"]
            self.initial_remaining = state["initial_remaining"]
            LOG.info("Resuming session=%s deliveries_created=%s", self.session, self.created)

    def save(self):
        self.path.parent.mkdir(parents=True, exist_ok=True)
        state = dict(version=1, session=self.session, customer_id=self.customer_id,
                     driver_ids=self.driver_ids, jobs=[asdict(j) for j in self.jobs.values()],
                     created=self.created, next_arrival=self.next_arrival,
                     initial_remaining=self.initial_remaining)
        temp = self.path.with_suffix(self.path.suffix + ".tmp")
        temp.write_text(json.dumps(state, indent=2))
        temp.replace(self.path)

    def bootstrap(self):
        # Readiness uses existing endpoints; the missing assignment probe should be 404.
        self.api.get("customer", "/api/customers")
        self.api.get("fleet", "/api/drivers")
        probe = "00000000-0000-0000-0000-000000000000"
        for service, path in [("delivery", f"/api/deliveries/{probe}"),
                              ("dispatch", f"/api/dispatch/assignments/{probe}")]:
            try:
                self.api.get(service, path)
            except ApiError as exc:
                if exc.status != 404:
                    raise
        if self.customer_id:
            # Do not silently continue stale IDs after an H2/stack reset.
            try:
                self.api.get("customer", f"/api/customers/{self.customer_id}")
            except ApiError as exc:
                if exc.status == 404:
                    raise ValueError("Saved customer no longer exists. Stop the simulator and remove its state volume after resetting the backend.") from exc
                raise
        else:
            customer = self.api.post("customer", "/api/customers", {
                "name": f"Demo customer {self.session}",
                "email": f"demo-{self.session}@example.com", "phone": "0400000000"})
            self.customer_id = customer["id"]
            self.save()
        # Save each creation separately so normal restarts reuse the owned pool.
        while len(self.driver_ids) < self.c.drivers:
            driver = self.api.post("fleet", "/api/drivers", {
                "name": f"Demo driver {len(self.driver_ids) + 1:02d} ({self.session})"})
            self.driver_ids.append(driver["id"])
            self.save()
            self.api.post("fleet", f"/api/drivers/{driver['id']}/availability", {"available": True})
        LOG.info("Ready session=%s customer=%s drivers=%s", self.session, self.customer_id, len(self.driver_ids))

    def after(self, seconds):
        return self.clock() + seconds / self.c.speed

    def available(self):
        reserved = {j.driver_id for j in self.jobs.values() if j.driver_id}
        return [d["id"] for d in self.api.get("fleet", "/api/drivers/available")
                if d["id"] in self.driver_ids and d["id"] not in reserved]

    def arrive(self):
        if self.created >= self.c.max_deliveries or len(self.jobs) >= self.c.max_tracked:
            return
        held = sum(not j.automated and j.status == "PENDING" for j in self.jobs.values())
        automated = held >= self.c.pending_target
        active = sum(j.automated and j.status not in TERMINAL for j in self.jobs.values())
        if automated and (active >= self.c.max_active or not self.available()):
            return
        # Schedule before POST. Never blindly retry non-idempotent delivery creation.
        self.next_arrival = self.after(self.c.arrival * self.rng.uniform(.7, 1.3))
        self.save()
        pickup, destination = self.rng.sample(ADDRESSES, 2)
        delivery = self.api.post("delivery", "/api/deliveries", {
            "customerId": self.customer_id, "pickupAddress": pickup,
            "destinationAddress": destination})
        draw = self.rng.random()
        outcome = "cancel" if draw < self.c.cancel_probability else (
            "delay" if draw < self.c.cancel_probability + self.c.delay_probability else "complete")
        job = Job(delivery["id"], automated, outcome, self.after(self.rng.uniform(3, 7)))
        self.jobs[job.delivery_id] = job
        self.created += 1
        self.initial_remaining = max(0, self.initial_remaining - 1)
        self.save()
        LOG.info("Created delivery=%s mode=%s outcome=%s", job.delivery_id,
                 "automatic" if automated else "human-controlled", outcome if automated else "human-controlled")

    def finish(self, job, delivery):
        driver_id = delivery.get("assignedDriverId")
        # Release only simulator-owned drivers, never drivers created by someone else.
        # All other tracked deliveries must also have finished before release.
        if self.c.recycle and driver_id in self.driver_ids:
            assignments = self.api.get("dispatch", f"/api/dispatch/drivers/{driver_id}/assignments")
            for assignment in assignments:
                if assignment["deliveryId"] == job.delivery_id:
                    continue
                current = self.api.get("delivery", f"/api/deliveries/{assignment['deliveryId']}")
                if current["status"] not in TERMINAL:
                    return
            driver = self.api.get("fleet", f"/api/drivers/{driver_id}")
            # A terminal event can precede Fleet's assignment consumer. Wait for it.
            if not job.fleet_observed and driver["availability"] != "UNAVAILABLE":
                if not job.warned:
                    LOG.warning("Waiting for Fleet assignment propagation before release: %s", job.delivery_id)
                    job.warned = True
                return
            self.api.post("fleet", f"/api/drivers/{driver_id}/availability", {"available": True})
            LOG.info("Recycled demo driver=%s", driver_id)
        del self.jobs[job.delivery_id]
        self.save()

    def advance(self, job):
        now = self.clock()
        delivery = self.api.get("delivery", f"/api/deliveries/{job.delivery_id}")
        status = delivery["status"]
        previous = job.status
        job.status = status
        actual_driver = delivery.get("assignedDriverId")
        if actual_driver and actual_driver in self.driver_ids:
            driver = self.api.get("fleet", f"/api/drivers/{actual_driver}")
            job.fleet_observed |= driver["availability"] == "UNAVAILABLE"
        if previous != status:
            LOG.info("Observed delivery=%s %s -> %s", job.delivery_id, previous, status)
        if status in TERMINAL:
            self.finish(job, delivery)
            return
        if not job.automated:
            return  # Human-controlled deliveries are observed, never advanced.
        if actual_driver and actual_driver != job.driver_id:
            job.automated = False
            job.driver_id = actual_driver
            LOG.info("Human assigned delivery=%s; relinquishing lifecycle control", job.delivery_id)
            return
        if status == "PENDING":
            if job.assignment_attempted:
                try:
                    self.api.get("dispatch", f"/api/dispatch/deliveries/{job.delivery_id}/assignment")
                except ApiError as exc:
                    if exc.status != 404:
                        raise
                # Even a timed-out POST may have persisted. Reserve the driver and
                # observe rather than retry or advance a delivery that is still pending.
                if now > job.due + 30 and not job.warned:
                    LOG.warning("Assignment stalled/uncertain delivery=%s; inspect services. No retry will be sent.", job.delivery_id)
                    job.warned = True
                return
            if now < job.due:
                return
            candidates = self.available()
            if not candidates:
                return
            job.driver_id = self.rng.choice(candidates)
            job.assignment_attempted = True
            job.due = now
            self.save()  # Journal intent before a potentially ambiguous POST result.
            try:
                self.api.post("dispatch", "/api/dispatch/assignments", {
                    "deliveryId": job.delivery_id, "driverId": job.driver_id, "vehicleId": None})
                LOG.info("Requested assignment delivery=%s driver=%s", job.delivery_id, job.driver_id)
            except ApiError as exc:
                if exc.status in {400, 404, 409, 422}:
                    job.assignment_attempted = False
                    job.driver_id = None
                    job.due = self.after(5)
                raise
            return
        if status == "ASSIGNED":
            if not job.fleet_observed:
                return
            if previous != status:
                job.due = self.after(self.rng.uniform(3, 7))
            if now >= job.due:
                if job.outcome == "cancel":
                    self.api.post("delivery", f"/api/deliveries/{job.delivery_id}/cancel", {
                        "reason": "CUSTOMER_REQUEST", "notes": "Demo scenario: customer changed plans"})
                else:
                    self.api.post("delivery", f"/api/deliveries/{job.delivery_id}/start")
                job.due = self.after(3)
            return
        if status == "IN_TRANSIT":
            if not job.complete_due:
                duration = self.c.transit * self.rng.uniform(.7, 1.3)
                job.complete_due = self.after(duration)
                job.due = self.after(duration / 2)
            if job.outcome == "delay" and not job.delay_attempted and now >= job.due:
                job.delay_attempted = True
                minutes = self.rng.choice([5, 10, 15, 25])
                job.complete_due += 15 / self.c.speed
                self.save()
                self.api.post("delivery", f"/api/deliveries/{job.delivery_id}/delay", {
                    "delayMinutes": minutes, "reason": "Demo scenario: heavy traffic"})
                LOG.info("Delayed delivery=%s minutes=%s", job.delivery_id, minutes)
            elif now >= job.complete_due:
                self.api.post("delivery", f"/api/deliveries/{job.delivery_id}/complete")

    def tick(self):
        for job in list(self.jobs.values()):
            try:
                self.advance(job)
            except ApiError as exc:
                LOG.warning("delivery=%s: %s", job.delivery_id, exc)
            self.save()
        if self.clock() >= self.next_arrival:
            try:
                self.arrive()
            except ApiError as exc:
                LOG.warning("Arrival failed; POST creation is not retried: %s", exc)
        # Spread the initial batch one arrival per tick; thereafter use normal cadence.
        if self.initial_remaining and self.created < self.c.max_deliveries:
            self.next_arrival = self.after(self.c.tick)
        self.save()


def main():
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    stop = threading.Event()
    for sig in (signal.SIGINT, signal.SIGTERM):
        signal.signal(sig, lambda *_: stop.set())
    try:
        simulator = Simulator(Config.from_env(), Api())
        # Bootstrap POSTs are not automatically retried: a timeout can hide success.
        deadline = time.monotonic() + number("SIM_STARTUP_TIMEOUT_SECONDS", 180, minimum=1)
        while not stop.is_set():
            try:
                simulator.bootstrap()
                break
            except ApiError as exc:
                if str(exc).startswith("POST ") or time.monotonic() >= deadline:
                    raise
                LOG.info("Waiting for APIs: %s", exc)
                stop.wait(3)
        while not stop.is_set():
            simulator.tick()
            stop.wait(simulator.c.tick)
        simulator.save()
        LOG.info("Stopped; scheduled work saved. Backend data retained.")
    except (ApiError, ValueError, OSError, KeyError) as exc:
        LOG.error("Simulator stopped: %s", exc)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
