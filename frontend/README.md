# DispatchAI operations workspace

Desktop React/TypeScript UI for the DispatchAI services. It uses React Query for service data, React Router for navigation, Tailwind/CSS for styling, and Recharts for analytics. The desktop layout has a persistent sidebar and supports 1280px and wider viewports; smaller screens can scroll rather than use a separate mobile layout.

## Run locally

Use Node 22.12+ (the Docker image uses Node 22), or Node 24. Run the services first, then:

```sh
cd frontend
npm ci
npm run dev
```

Open `http://localhost:3000`. The Vite server proxies these relative API prefixes:

| Prefix           | Environment variable    | Local default           |
| ---------------- | ----------------------- | ----------------------- |
| `/customer-api`  | `CUSTOMER_SERVICE_URL`  | `http://localhost:8084` |
| `/delivery-api`  | `DELIVERY_SERVICE_URL`  | `http://localhost:8081` |
| `/fleet-api`     | `FLEET_SERVICE_URL`     | `http://localhost:8082` |
| `/dispatch-api`  | `DISPATCH_SERVICE_URL`  | `http://localhost:8083` |
| `/analytics-api` | `ANALYTICS_SERVICE_URL` | `http://localhost:8085` |
| `/agent-api`     | `AGENT_SERVICE_URL`     | `http://localhost:8086` |

The root Compose file supplies container hostnames for these proxies. Rebuild the frontend with `docker compose up -d --build frontend` after applying the patch. The Compose health check uses the Node runtime supplied by the image.

```sh
npm run lint
npm run build
```

The production build creates `dist/`. A static deployment must serve `index.html` for application routes and forward the six API prefixes above to their services. `vite preview` is a local build preview, not a production reverse proxy. The supplied Compose configuration continues to run the Vite server.

## Workspace

| Route                     | Purpose                                                                                                                     |
| ------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| `/dashboard`              | Delivery totals, active work and delays, recent throughput, lifecycle mix, resource readiness                               |
| `/deliveries`             | Search, status filters, pagination, delivery creation with an existing or new customer                                      |
| `/deliveries/:deliveryId` | Authoritative delivery state, addresses and parcel weight, projected timeline, assignment and lifecycle actions             |
| `/dispatch`               | Pending queue, weight correction, eligible drivers and vehicles, manual or automatic assignment, optional AI recommendation |
| `/fleet`                  | Driver and vehicle directories, availability controls, creation, driver performance and recent work                         |
| `/analytics`              | Backend history charts, lifetime outcomes, average assignment, delivery, transit and reported delay                         |
| `/assistant`              | User-triggered operational questions, with completed exchanges retained in this browser tab's session                       |

Legacy `/drivers` links redirect to `/fleet` and retain query parameters. Chart range and delivery filters live in the URL. The top-bar search searches deliveries by customer, driver or identifier.

## Data semantics

- Operational queries refresh every five seconds while their screen is mounted; customer lookups refresh every fifteen seconds. The refresh button invalidates cached queries. Displayed data comes from the services; production code contains no demo fixtures.
- Throughput uses the backend's one-minute created/completed buckets. Delay rate uses rolling five-minute windows, reported each minute: distinct delayed deliveries divided by distinct deliveries with activity. A window without activity has no rate and remains a gap rather than a false 0%. Choose 15 minutes, one hour or three hours; navigation and reload fetch history from the backend.
- Chart times use the browser's local timezone, shown in the footer. The current minute is incomplete, and late events may revise recent history. Chart data is also available in expandable tables.
- Lifetime completion, cancellation and ever-delayed ratios use all recorded deliveries as their denominator. Active delays on the overview only count open deliveries. A completed delivery may also have been delayed, so these outcome ratios do not form a single partition.
- Lifecycle commands use Delivery's authoritative state. Analytics supplies reporting timestamps and may lag behind new deliveries or status changes. Assignment submission stays in a waiting state until Delivery confirms it, and rejected assignments can be retried.
- Vehicle eligibility checks actual parcel weight and available capacity. AI advice is requested only when the user clicks; it does not assign resources. Automatic assignment explicitly submits the backend's best-fit choice. Eligibility is refreshed so changed resources are not silently reused.
- Failed reads show unavailable or stale-data notices. Empty results are distinct from failed requests. Fleet always exposes both Add driver and Add vehicle actions. The vehicle form can mark a newly created vehicle available immediately (checked by default); clear the option to keep it unavailable. If readiness fails after creation, retry availability on the saved vehicle without creating another. New drivers start unavailable and have explicit readiness controls.

## Demo data

From the project root, run `python3 scripts/seed-demo.py` to add demo customers, ready drivers/vehicles, completed and cancelled history, active deliveries and pending work. This replaces `scripts/test-dispatchai.sh` and does not call an LLM. See `scripts/README.md` for counts, URL overrides, slow chart activity, and partial-run journals.

## Desktop browser smoke test

`tests/desktop-smoke.cjs` exercises the production bundle with contract-shaped service fixtures. It covers navigation, Recharts rendering and range selection, filtering, delivery and resource creation, invalid weight, stale advice, delayed assignment confirmation, rejection/retry, availability controls, assistant persistence, cancellation, unavailable services, empty data, and 1280px page overflow. It does not start a server or contact real services or models.

Install Playwright separately if you want to run it (it is not a production dependency):

```sh
npm install --no-save --package-lock=false playwright
npx playwright install chromium
npm run build
node tests/desktop-smoke.cjs
```

The smoke test was verified with Playwright 1.51.1 and Chromium 134. It writes screenshots and `results.json` into the OS temporary directory under `dispatchai-desktop-smoke`; override this with `TEST_RESULTS_DIR`. Use `PLAYWRIGHT_MODULE` for an externally installed Playwright module.

Live end-to-end acceptance still requires the Spring/Kafka services: create a delivery, confirm the asynchronous projection appears, assign a fitting pair, progress the delivery, and confirm analytics and resource release. Mocked browser checks do not validate the running service stack.
