const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");
const fs = require("fs");
const assert = require("assert");
const target = "http://localhost:3300";
const pathUtil = require("path");
const dist = pathUtil.resolve(__dirname, "../dist");
const out =
  process.env.TEST_RESULTS_DIR ||
  pathUtil.join(require("os").tmpdir(), "dispatchai-desktop-smoke");
fs.mkdirSync(out, { recursive: true });
const uuid = (n) =>
  `${(0xa2b39000 + n).toString(16)}-0000-4000-8000-${String(n).padStart(12, "0")}`;
const now = Date.now();
const iso = (m) => new Date(now - m * 60000).toISOString();
const customers = [
  {
    id: uuid(1),
    name: "Apex Distribution",
    email: "dispatch@apex.example",
    phone: "0400000000",
  },
  {
    id: uuid(2),
    name: "Harbour Supply Co.",
    email: "ops@harbour.example",
    phone: "0400000001",
  },
  {
    id: uuid(3),
    name: "Metro Retail Group",
    email: "deliveries@metro.example",
    phone: "0400000002",
  },
];
const drivers = Array.from({ length: 8 }, (_, i) => ({
  id: uuid(100 + i),
  name: [
    "Alex Morgan",
    "Jamie Chen",
    "Sam Wilson",
    "Taylor Reed",
    "Jordan Lee",
    "Casey Patel",
    "Riley Davis",
    "Avery Smith",
  ][i],
  availability: i < 5 ? "AVAILABLE" : "UNAVAILABLE",
}));
const vehicles = [
  {
    id: uuid(201),
    registration: "DAI-102",
    type: "VAN",
    capacityKg: 100,
    available: true,
  },
  {
    id: uuid(202),
    registration: "DAI-204",
    type: "VAN",
    capacityKg: 500,
    available: true,
  },
  {
    id: uuid(203),
    registration: "DAI-305",
    type: "TRUCK",
    capacityKg: 1500,
    available: true,
  },
  {
    id: uuid(204),
    registration: "DAI-401",
    type: "CAR",
    capacityKg: 50,
    available: false,
  },
];
let deliveries = Array.from({ length: 32 }, (_, i) => {
  const status =
    i < 4
      ? "PENDING"
      : i < 8
        ? "ASSIGNED"
        : i < 14
          ? "IN_TRANSIT"
          : i < 29
            ? "COMPLETED"
            : "CANCELLED";
  return {
    deliveryId: uuid(1000 + i),
    customerId: customers[i % 3].id,
    driverId: status === "PENDING" ? null : drivers[i % 8].id,
    status,
    delayed: [2, 9, 11, 17, 20].includes(i),
    delayMinutes: [2, 9, 11, 17, 20].includes(i) ? 15 + i : null,
    delayReason: "Heavy traffic near the destination",
    createdAt: iso(i * 3 + 3),
    assignedAt: status === "PENDING" ? null : iso(i * 3 + 2),
    startedAt: ["IN_TRANSIT", "COMPLETED"].includes(status)
      ? iso(i * 3 + 1)
      : null,
    completedAt: status === "COMPLETED" ? iso(i * 3) : null,
    cancelledAt: status === "CANCELLED" ? iso(i * 3) : null,
    lastDelayAt: [2, 9, 11, 17, 20].includes(i) ? iso(i * 3 + 1) : null,
  };
});
const source = new Map(
  deliveries.map((d) => [
    d.deliveryId,
    {
      id: d.deliveryId,
      customerId: d.customerId,
      assignedDriverId: d.driverId,
      pickupAddress: [
        "12 King Street, Sydney NSW",
        "85 George Street, Parramatta NSW",
        "2 Wharf Road, Pyrmont NSW",
      ][Number(d.deliveryId.slice(-2)) % 3],
      destinationAddress: [
        "28 Victoria Road, Ryde NSW",
        "145 Pacific Highway, Chatswood NSW",
        "64 Church Street, Parramatta NSW",
      ][Number(d.deliveryId.slice(-2)) % 3],
      parcelWeightKg: 32.5,
      status: d.status,
      delayed: d.delayed,
      delayMinutes: d.delayMinutes,
      delayReason: d.delayed ? d.delayReason : null,
    },
  ]),
);
let assignments = new Map();
let calls = [];
let offline = false;
let empty = false;
let slow = false;
let pendingHold = null;
let rejectNext = false;
let failVehicleReadiness = false;
function summary() {
  const all = empty ? [] : deliveries;
  const count = (status) => all.filter((d) => d.status === status).length;
  const total = all.length;
  return {
    total,
    pending: count("PENDING"),
    assigned: count("ASSIGNED"),
    inTransit: count("IN_TRANSIT"),
    completed: count("COMPLETED"),
    cancelled: count("CANCELLED"),
    delayed: all.filter((d) => d.delayed).length,
    averageDelayMinutes: 18.2,
    averageAssignmentTimeSeconds: 47,
    averageDeliveryTimeSeconds: 1044,
    averageTransitTimeSeconds: 825,
    completionRate: total ? count("COMPLETED") / total : 0,
    cancellationRate: total ? count("CANCELLED") / total : 0,
    delayRate: total ? all.filter((d) => d.delayed).length / total : 0,
  };
}
function history(minutes, delay) {
  const t = Math.floor(Date.now() / 60000) * 60000;
  return Array.from({ length: minutes }, (_, i) => {
    const x = i + 1;
    const active = i % 13 === 0 ? 0 : 9 + (i % 5);
    const delayed = active ? i % 4 : 0;
    return {
      start: new Date(
        t - (minutes - 1 - i + (delay ? 5 : 0)) * 60000,
      ).toISOString(),
      end: new Date(
        t - (minutes - 1 - i + (delay ? 0 : -1)) * 60000,
      ).toISOString(),
      created: empty
        ? 0
        : Math.max(
            0,
            Math.round(5 + 3 * Math.sin(x * 0.29) + 2 * Math.cos(x * 0.7)),
          ),
      completed: empty
        ? 0
        : Math.max(
            0,
            Math.round(3 + 2 * Math.sin(x * 0.27 - 1) + Math.cos(x * 0.8)),
          ),
      activeDeliveries: empty ? 0 : active,
      delayedDeliveries: empty ? 0 : delayed,
      delayRate: empty || !active ? null : delayed / active,
    };
  });
}
function performance(id) {
  return {
    driverId: id,
    totalAssigned: 24,
    completed: 19,
    cancelled: 2,
    delayed: 3,
    averageDeliveryTimeSeconds: 920,
    averageTransitTimeSeconds: 730,
  };
}
(async () => {
  const browser = await chromium.launch({
    headless: true,
    args: ["--no-sandbox"],
  });
  const page = await browser.newPage({
    viewport: { width: 1440, height: 1100 },
    deviceScaleFactor: 1,
    timezoneId: "Australia/Sydney",
  });
  let errors = [];
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("console", (msg) => {
    if (
      msg.type() === "error" &&
      !msg.text().includes("Failed to load resource")
    )
      errors.push(msg.text());
  });
  await page.route(target + "/**", async (route) => {
    const pathname = new URL(route.request().url()).pathname;
    let file = pathUtil.join(dist, decodeURIComponent(pathname));
    if (!fs.existsSync(file) || fs.statSync(file).isDirectory())
      file = pathUtil.join(dist, "index.html");
    const ext = pathUtil.extname(file);
    const contentType =
      ext === ".js"
        ? "application/javascript"
        : ext === ".css"
          ? "text/css"
          : ext === ".svg"
            ? "image/svg+xml"
            : "text/html";
    return route.fulfill({ path: file, contentType });
  });
  await page.route("**/*-api/**", async (route) => {
    const req = route.request();
    const url = new URL(req.url());
    const path = url.pathname;
    const method = req.method();
    const body = req.postData() ? JSON.parse(req.postData()) : null;
    calls.push({ path, method, body });
    const ok = (data, status = 200) =>
      route.fulfill({
        status,
        contentType: "application/json",
        body: JSON.stringify(data),
      });
    if (offline && path.startsWith("/analytics-api"))
      return ok({ message: "Analytics temporarily unavailable" }, 503);
    if (path === "/customer-api/api/customers") {
      if (method === "POST") {
        const item = { ...body, id: uuid(50 + customers.length) };
        customers.push(item);
        return ok(item, 201);
      }
      return ok(empty ? [] : customers);
    }
    if (path === "/fleet-api/api/drivers/available")
      return ok(
        empty ? [] : drivers.filter((d) => d.availability === "AVAILABLE"),
      );
    if (path === "/fleet-api/api/drivers") {
      if (method === "POST") {
        const item = {
          ...body,
          id: uuid(120 + drivers.length),
          availability: "UNAVAILABLE",
        };
        drivers.push(item);
        return ok(item, 201);
      }
      return ok(empty ? [] : drivers);
    }
    if (path.includes("/fleet-api/api/drivers/")) {
      const id = path.split("/")[4];
      const driver = drivers.find((d) => d.id === id);
      if (!driver) return ok({ message: "Driver not found" }, 404);
      if (method === "POST")
        driver.availability = body.available ? "AVAILABLE" : "UNAVAILABLE";
      return ok(driver);
    }
    if (path === "/fleet-api/api/vehicles") {
      if (method === "POST") {
        const item = {
          ...body,
          id: uuid(220 + vehicles.length),
          available: false,
        };
        vehicles.push(item);
        return ok(item, 201);
      }
      return ok(empty ? [] : vehicles);
    }
    if (path.includes("/fleet-api/api/vehicles/")) {
      const id = path.split("/")[4];
      const v = vehicles.find((v) => v.id === id);
      if (method === "POST") {
        if (failVehicleReadiness) {
          failVehicleReadiness = false;
          return ok({ message: "Fleet temporarily unavailable" }, 503);
        }
        v.available = body.available;
      }
      return ok(v);
    }
    if (path === "/analytics-api/api/analytics/deliveries/summary")
      return ok(summary());
    if (path.endsWith("/throughput") || path.endsWith("/delay-rate"))
      return ok(
        history(
          Number(url.searchParams.get("minutes") ?? 60),
          path.endsWith("/delay-rate"),
        ),
      );
    if (path.includes("/analytics-api/api/analytics/deliveries/drivers/"))
      return ok(performance(path.split("/").pop()));
    if (path === "/analytics-api/api/analytics/deliveries")
      return ok(empty ? [] : deliveries);
    if (path.startsWith("/analytics-api/api/analytics/deliveries/")) {
      const d = deliveries.find((d) => d.deliveryId === path.split("/").pop());
      return d ? ok(d) : ok({ message: "Not projected yet" }, 404);
    }
    if (path === "/delivery-api/api/deliveries" && method === "POST") {
      const id = uuid(2000 + source.size);
      const d = {
        ...body,
        id,
        assignedDriverId: null,
        status: "PENDING",
        delayed: false,
        delayMinutes: null,
        delayReason: null,
      };
      source.set(id, d);
      return ok(d, 201);
    }
    if (path.startsWith("/delivery-api/api/deliveries/")) {
      const id = path.split("/")[4];
      const d = source.get(id);
      if (!d) return ok({ message: "Delivery not found" }, 404);
      if (pendingHold?.deliveryId === id && ++pendingHold.polls >= 3) {
        d.status = "ASSIGNED";
        d.assignedDriverId = pendingHold.driverId;
        const projection = deliveries.find((x) => x.deliveryId === id);
        if (projection) {
          projection.status = "ASSIGNED";
          projection.driverId = pendingHold.driverId;
        }
        pendingHold = null;
      }
      if (method === "PATCH") {
        d.parcelWeightKg = body.parcelWeightKg;
        return ok(d);
      }
      if (method === "POST") {
        const action = path.split("/").pop();
        if (action === "cancel") {
          d.status = "CANCELLED";
          const p = deliveries.find((x) => x.deliveryId === id);
          if (p) {
            p.status = "CANCELLED";
            p.cancelledAt = iso(0);
          }
        }
        if (action === "start") d.status = "IN_TRANSIT";
        if (action === "complete") d.status = "COMPLETED";
        if (action === "delay") {
          d.delayed = true;
          d.delayMinutes = body.delayMinutes;
          d.delayReason = body.reason;
        }
        return ok(d);
      }
      return ok(d);
    }
    if (path.endsWith("/eligibility")) {
      const id = path.split("/").at(-2);
      const d = source.get(id);
      if (!d || d.status !== "PENDING")
        return ok({ message: "Delivery is no longer pending" }, 409);
      return ok({
        deliveryId: id,
        parcelWeightKg: d.parcelWeightKg,
        availableDrivers: drivers.filter((d) => d.availability === "AVAILABLE"),
        eligibleVehicles: vehicles.filter(
          (v) => v.available && v.capacityKg >= d.parcelWeightKg,
        ),
      });
    }
    if (
      path === "/dispatch-api/api/dispatch/assignments" ||
      path.endsWith("/assignments/auto")
    ) {
      const d = source.get(body.deliveryId);
      const driverId = body.driverId ?? drivers[0].id;
      const vehicleId = body.vehicleId ?? vehicles[0].id;
      const id = uuid(3000 + assignments.size);
      const status = rejectNext ? "REJECTED" : "ACTIVE";
      rejectNext = false;
      const a = { id, deliveryId: d.id, driverId, vehicleId, status };
      assignments.set(id, a);
      if (status === "ACTIVE") {
        if (slow) pendingHold = { deliveryId: d.id, driverId, polls: 0 };
        else {
          d.status = "ASSIGNED";
          d.assignedDriverId = driverId;
          const projection = deliveries.find((item) => item.deliveryId === d.id);
          if (projection) {
            projection.status = "ASSIGNED";
            projection.driverId = driverId;
          }
        }
      }
      return ok(a, 201);
    }
    if (path.includes("/dispatch-api/api/dispatch/assignments/"))
      return ok(assignments.get(path.split("/").pop()));
    if (path.endsWith("/assignment")) {
      const id = path.split("/").at(-2);
      const a = [...assignments.values()].find((a) => a.deliveryId === id);
      return a ? ok(a) : ok({ message: "No assignment" }, 404);
    }
    if (path === "/agent-api/api/agent/dispatch/advice") {
      const d = source.get(body.deliveryId);
      const vehicle = vehicles.find(
        (v) => v.available && v.capacityKg >= d.parcelWeightKg,
      );
      return ok({
        recommendedDriverId: drivers[0].id,
        recommendedDriverName: drivers[0].name,
        reasoning:
          "Alex has completed 19 of 24 assignments. DAI-102 has sufficient capacity for this parcel and preserves larger vehicles for heavier deliveries.",
        recommendedVehicleId: vehicle.id,
        recommendedVehicleRegistration: vehicle.registration,
        parcelWeightKg: d.parcelWeightKg,
        vehicleCapacityKg: vehicle.capacityKg,
        candidates: drivers
          .filter((d) => d.availability === "AVAILABLE")
          .map((d) => ({ driver: d, analytics: performance(d.id) })),
        eligibleVehicles: vehicles.filter(
          (v) => v.available && v.capacityKg >= d.parcelWeightKg,
        ),
      });
    }
    if (path === "/agent-api/api/agent/operations/query")
      return ok({
        response:
          "There are 32 deliveries recorded. Four are waiting for dispatch, ten are active, and two active deliveries have reported delays. Five drivers and three vehicles are ready for new work. Review the pending queue and reported delays first.",
      });
    return ok({ message: "Unhandled fixture route " + path }, 404);
  });
  await page.goto(target + "/dashboard");
  await page.getByRole("heading", { name: "Operations overview" }).waitFor();
  await page.getByText("Apex Distribution", { exact: true }).first().waitFor();
  await page.waitForTimeout(700);
  await page.screenshot({ path: out + "/overview.png", fullPage: true });
  assert(
    (await page.locator(".recharts-surface").count()) > 0,
    "Recharts rendered",
  );
  const initialAi = calls.filter((c) => c.path.includes("/agent-api/")).length;
  assert.equal(initialAi, 0, "No automatic inference");
  await page.getByRole("link", { name: "Analytics", exact: true }).click();
  await page.getByRole("heading", { name: "Analytics", exact: true }).waitFor();
  await page.getByRole("button", { name: "3 hours", exact: true }).click();
  await page.waitForTimeout(300);
  assert(calls.some((c) => c.path.endsWith("/throughput")));
  await page.screenshot({ path: out + "/analytics.png", fullPage: true });
  await page
    .getByRole("link", { name: "Deliveries", exact: true })
    .first()
    .click();
  await page
    .getByRole("heading", { name: "Deliveries", exact: true })
    .waitFor();
  await page.getByRole("textbox", { name: "Filter deliveries" }).fill("Apex");
  await page.waitForTimeout(200);
  assert((await page.locator("tbody tr").count()) > 0);
  await page.screenshot({ path: out + "/deliveries.png", fullPage: true });
  await page.getByRole("button", { name: "New delivery", exact: true }).click();
  const modal = page.getByRole("dialog");
  await modal
    .locator('select[name="customerId"]')
    .selectOption(customers[0].id);
  await modal.getByLabel("Pickup address").fill("12 King Street, Sydney");
  await modal
    .getByLabel("Destination address")
    .fill("45 George Street, Parramatta");
  await modal.getByLabel("Parcel weight (kg)").fill("0");
  const creationCount = calls.filter(
    (c) => c.path === "/delivery-api/api/deliveries" && c.method === "POST",
  ).length;
  await modal
    .getByRole("button", { name: "Create delivery", exact: true })
    .click();
  assert.equal(
    calls.filter(
      (c) => c.path === "/delivery-api/api/deliveries" && c.method === "POST",
    ).length,
    creationCount,
    "Invalid weight is not submitted",
  );
  await modal.getByLabel("Parcel weight (kg)").fill("25.5");
  await modal
    .getByRole("button", { name: "Create delivery", exact: true })
    .click();
  await page
    .getByRole("heading", { name: /Delivery #/ })
    .first()
    .waitFor();
  assert(
    calls.some(
      (c) =>
        c.method === "POST" &&
        c.path === "/delivery-api/api/deliveries" &&
        c.body.parcelWeightKg === 25.5,
    ),
  );
  await page.waitForTimeout(200);
  await page.screenshot({ path: out + "/delivery-detail.png", fullPage: true });
  await page.locator('aside a.nav-link[href="/dispatch"]').click();
  await page.goto(target + "/dispatch?delivery=" + uuid(1000));
  await page
    .getByRole("button", { name: "Get AI advice", exact: true })
    .waitFor();
  await page
    .getByRole("button", { name: "Get AI advice", exact: true })
    .click();
  await page.getByText("Dispatch recommendation", { exact: true }).waitFor();
  await page.screenshot({ path: out + "/dispatch.png", fullPage: true });
  vehicles[0].available = false;
  await page.getByRole("button", { name: "Refresh operational data" }).click();
  await page
    .getByText(
      "This recommendation is no longer current. Request fresh advice.",
    )
    .waitFor();
  assert(
    await page
      .getByRole("button", { name: "Assign selected pair", exact: true })
      .isDisabled(),
  );
  vehicles[0].available = true;
  await page.getByRole("button", { name: "Refresh operational data" }).click();
  await page.waitForTimeout(200);
  slow = true;
  await page
    .getByRole("button", { name: "Assign selected pair", exact: true })
    .click();
  await page
    .getByText("Assignment submitted. Waiting for delivery confirmation.")
    .waitFor();
  await page
    .getByText("This delivery is no longer waiting for dispatch.")
    .waitFor({ timeout: 25000 });
  const assignmentCall = calls.find(
    (c) =>
      c.path === "/dispatch-api/api/dispatch/assignments" &&
      c.method === "POST",
  );
  assert.equal(assignmentCall.body.vehicleId, vehicles[0].id);
  await page.getByRole("link", { name: "Fleet", exact: true }).first().click();
  await page.getByRole("heading", { name: "Fleet", exact: true }).waitFor();
  await page
    .getByRole("button", { name: "Mark available", exact: true })
    .first()
    .waitFor();
  await page.getByRole("button", { name: "Add driver", exact: true }).click();
  await page
    .getByRole("dialog")
    .getByLabel("Driver name")
    .fill("New Demo Driver");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Add driver", exact: true })
    .click();
  await page
    .getByRole("heading", { name: "New Demo Driver", exact: true })
    .waitFor();
  assert(
    calls.some(
      (c) =>
        c.path === "/fleet-api/api/drivers" &&
        c.method === "POST" &&
        c.body.name === "New Demo Driver",
    ),
  );
  await page.screenshot({ path: out + "/fleet.png", fullPage: true });
  await page.getByRole("button", { name: "Add vehicle", exact: true }).click();
  await page.getByRole("dialog").getByLabel("Registration").fill("DEMO-NEW");
  await page
    .getByRole("dialog")
    .getByLabel("Vehicle type")
    .selectOption("TRUCK");
  const vehiclePostsBefore = calls.filter(
    (c) => c.path === "/fleet-api/api/vehicles" && c.method === "POST",
  ).length;
  await page.getByRole("dialog").getByLabel("Capacity (kg)").fill("0");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Add vehicle", exact: true })
    .click();
  assert.equal(
    calls.filter(
      (c) => c.path === "/fleet-api/api/vehicles" && c.method === "POST",
    ).length,
    vehiclePostsBefore,
  );
  await page.getByRole("dialog").getByLabel("Capacity (kg)").fill("750");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Add vehicle", exact: true })
    .click();
  await page.getByText("DEMO-NEW", { exact: true }).waitFor();
  assert(
    calls.some(
      (c) =>
        c.path === "/fleet-api/api/vehicles" &&
        c.method === "POST" &&
        c.body.capacityKg === 750 &&
        c.body.type === "TRUCK",
    ),
  );
  await page.getByRole("dialog").waitFor({ state: "hidden" });
  assert(
    vehicles.find((v) => v.registration === "DEMO-NEW").available,
    "New vehicle made ready",
  );
  assert(
    page.url().includes("view=vehicles"),
    "Creation opens vehicle directory",
  );
  await page.getByRole("button", { name: /^Vehicles/ }).click();
  await page
    .getByRole("button", { name: "Mark DAI-102 unavailable", exact: true })
    .click();
  await page.getByText("DAI-102: availability updated.").waitFor();
  await page.screenshot({ path: out + "/vehicles.png", fullPage: true });
  failVehicleReadiness = true;
  await page.getByRole("button", { name: "Add vehicle", exact: true }).click();
  await page.getByRole("dialog").getByLabel("Registration").fill("DEMO-RETRY");
  await page.getByRole("dialog").getByLabel("Capacity (kg)").fill("500");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Add vehicle", exact: true })
    .click();
  await page
    .getByText(/DEMO-RETRY was created. Availability could not be confirmed/)
    .waitFor();
  const creationCountBeforeRetry = calls.filter(
    (c) => c.path === "/fleet-api/api/vehicles" && c.method === "POST",
  ).length;
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Retry availability", exact: true })
    .click();
  await page.getByRole("dialog").waitFor({ state: "hidden" });
  assert.equal(
    calls.filter(
      (c) => c.path === "/fleet-api/api/vehicles" && c.method === "POST",
    ).length,
    creationCountBeforeRetry,
    "Retry does not create a duplicate vehicle",
  );
  assert(vehicles.find((v) => v.registration === "DEMO-RETRY").available);
  await page.getByRole("button", { name: "Add vehicle", exact: true }).click();
  await page
    .getByRole("dialog")
    .getByLabel("Registration")
    .fill("DEMO-OFFLINE");
  await page.getByRole("dialog").getByLabel("Capacity (kg)").fill("50");
  await page
    .getByRole("dialog")
    .getByLabel("Make available for dispatch")
    .uncheck();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Add vehicle", exact: true })
    .click();
  await page.getByRole("dialog").waitFor({ state: "hidden" });
  assert.equal(
    vehicles.find((v) => v.registration === "DEMO-OFFLINE").available,
    false,
    "Can create an unavailable vehicle",
  );
  slow = false;
  await page.goto(target + "/dispatch?delivery=" + uuid(1003));
  await page.getByRole("button", { name: /Jamie Chen/ }).click();
  const newVehicleId = vehicles.find((v) => v.registration === "DEMO-NEW").id;
  await page
    .getByLabel("Eligible vehicle", { exact: true })
    .selectOption(newVehicleId);
  await page
    .getByRole("button", { name: "Assign selected pair", exact: true })
    .click();
  await page
    .getByText("This delivery is no longer waiting for dispatch.")
    .waitFor();
  assert(
    calls.some(
      (c) =>
        c.path === "/dispatch-api/api/dispatch/assignments" &&
        c.method === "POST" &&
        c.body.deliveryId === uuid(1003) &&
        c.body.vehicleId === newVehicleId,
    ),
    "Newly created vehicle can be used in a delivery assignment",
  );
  await page.getByRole("link", { name: "AI assistant", exact: true }).click();
  await page.getByRole("button", { name: /Operational briefing/ }).click();
  await page.getByText(/There are 32 deliveries recorded/).waitFor();
  await page.screenshot({ path: out + "/assistant.png", fullPage: true });
  await page.getByRole("link", { name: "Analytics", exact: true }).click();
  await page.getByRole("link", { name: "AI assistant", exact: true }).click();
  await page.getByText(/There are 32 deliveries recorded/).waitFor();
  assert.equal(
    calls.filter((c) => c.path === "/agent-api/api/agent/operations/query")
      .length,
    1,
  );
  await page.goto(target + "/deliveries/" + uuid(1001));
  await page
    .getByRole("button", { name: "Cancel delivery", exact: true })
    .waitFor();
  await page
    .getByRole("button", { name: "Cancel delivery", exact: true })
    .click();
  await page.getByRole("dialog", { name: "Cancel delivery" }).waitFor();
  await page.keyboard.press("Escape");
  assert.equal(calls.filter((c) => c.path.endsWith("/cancel")).length, 0);
  await page
    .getByRole("button", { name: "Cancel delivery", exact: true })
    .click();
  await page
    .getByRole("dialog", { name: "Cancel delivery" })
    .getByRole("button", { name: "Confirm cancellation" })
    .click();
  await page
    .getByText(
      "This delivery has finished. No further lifecycle actions are available.",
    )
    .waitFor();
  await page.goto(target + "/dispatch?delivery=" + uuid(1002));
  rejectNext = true;
  await page
    .getByRole("button", { name: "Auto assign best fit", exact: true })
    .click();
  await page.getByText(/previous assignment was rejected/).waitFor();
  assert(
    await page
      .getByRole("button", { name: "Auto assign best fit", exact: true })
      .isEnabled(),
  );
  await page.setViewportSize({ width: 1280, height: 900 });
  for (const [route, heading] of [
    ["/dashboard", "Operations overview"],
    ["/analytics", "Analytics"],
    ["/deliveries", "Deliveries"],
    ["/fleet", "Fleet"],
    ["/dispatch", "Dispatch"],
    ["/assistant", "Operations assistant"],
  ]) {
    await page.goto(target + route);
    await page.getByRole("heading", { name: heading, exact: true }).waitFor();
    if (route === "/dashboard" || route === "/deliveries")
      await page
        .getByText("Apex Distribution", { exact: true })
        .first()
        .waitFor();
    if (route === "/analytics")
      await page.locator(".recharts-surface").first().waitFor();
    if (route === "/fleet")
      await page
        .getByRole("heading", { name: "Alex Morgan", exact: true })
        .waitFor();
    if (route === "/dispatch")
      await page
        .getByRole("button", { name: "Get AI advice", exact: true })
        .waitFor();
    if (route === "/assistant")
      await page.getByText(/There are 32 deliveries recorded/).waitFor();
    await page.evaluate(() => document.fonts.ready);
    await page.waitForTimeout(300);
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth > innerWidth + 1,
    );
    assert(!overflow, "No page overflow at 1280: " + route);
    await page.screenshot({ path: out + route + "-1280.png", fullPage: true });
  }
  offline = true;
  await page.goto(target + "/dashboard");
  await page
    .getByText(/Some operational data could not be refreshed/)
    .waitFor({ timeout: 15000 });
  await page.screenshot({ path: out + "/partial-outage.png", fullPage: true });
  await page.goto(target + "/dispatch");
  await page
    .getByRole("heading", { name: "Dispatch queue unavailable", exact: true })
    .waitFor({ timeout: 15000 });
  assert.equal(
    await page
      .getByRole("heading", { name: "The queue is clear", exact: true })
      .count(),
    0,
    "Failed queue is not reported clear",
  );
  offline = false;
  const availabilitySnapshot = vehicles.map((v) => v.available);
  vehicles.forEach((v) => (v.available = false));
  await page.goto(target + "/dispatch?delivery=" + uuid(1002));
  await page.getByText("No available vehicle can carry this parcel.").waitFor();
  assert(
    await page
      .getByRole("button", { name: "Get AI advice", exact: true })
      .isDisabled(),
  );
  assert(
    await page
      .getByRole("button", { name: "Auto assign best fit", exact: true })
      .isDisabled(),
  );
  await page.getByRole("link", { name: "Add vehicle", exact: true }).click();
  await page
    .getByRole("dialog", { name: "Add a vehicle", exact: true })
    .waitFor();
  assert(
    page.url().includes("create=vehicle"),
    "Dispatch links directly to vehicle creation",
  );
  await page.keyboard.press("Escape");
  vehicles.forEach((v, i) => (v.available = availabilitySnapshot[i]));
  empty = true;
  await page.goto(target + "/dashboard");
  await page
    .getByRole("heading", { name: "Your first delivery starts here" })
    .waitFor();
  await page.screenshot({ path: out + "/empty.png", fullPage: true });
  assert.deepEqual(errors, [], "No browser runtime errors");
  fs.writeFileSync(
    out + "/results.json",
    JSON.stringify(
      {
        passed: true,
        requests: calls.length,
        modelCalls: calls.filter((c) => c.path.includes("/agent-api/")).length,
        errors,
        screens: [
          "overview",
          "analytics",
          "deliveries",
          "delivery-detail",
          "dispatch",
          "fleet",
          "vehicles",
          "assistant",
          "partial-outage",
          "empty",
        ],
      },
      null,
      2,
    ),
  );
  console.log(
    "Desktop smoke passed: navigation, charts, filtering, creation, AI advice, delayed confirmation, rejection, fleet readiness, conversation, cancellation, empty/outage states, 1280px overflow.",
  );
  await browser.close();
})().catch((e) => {
  console.error(e);
  process.exit(1);
});
