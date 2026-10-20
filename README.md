# DispatchAI

DispatchAI is an AI-assisted parcel logistics prototype. Operators can manage customers, drivers and vehicles; assign suitable resources to deliveries; track delivery progress and delays; and inspect streaming analytics. A local LLM supports operational questions and dispatch recommendations, with assignment validation retained in the application services.

The application uses six Spring Boot services, Kafka and Kafka Streams, service-owned H2 databases, LangChain4j with Ollama, and a React/TypeScript desktop frontend with Recharts.

All commands below assume a POSIX-compatible terminal opened at the repository root, containing `compose.yaml` and the service directories. On Windows, use WSL or adapt the shell commands to your environment.

## Prerequisites

| Requirement                                                              | Purpose                                                                   |
| ------------------------------------------------------------------------ | ------------------------------------------------------------------------- |
| Git                                                                      | Clone the repository                                                      |
| Docker Engine or Docker Desktop and the Docker Compose plugin            | Build and run the application, Kafka and Ollama                           |
| Python 3.10 or later                                                     | Run the demo seeder and Python checks; no pip packages are required       |
| Browser                                                                  | Access the desktop interface                                              |
| curl or another REST client                                              | Run the API checks and examples                                           |
| JDK 21, for local Java development                                       | Build, test or run services outside containers using their Maven wrappers |
| Node.js 22.12 or later in the Node 22 release line, or Node 24, with npm | Develop and build the frontend outside Docker; its image uses Node 22     |

Allow sufficient RAM, GPU memory and disk space for the services, container images and `llama3.1:8b`. First startup requires network access to download images, dependencies and the model. Java and Node.js do not need to be installed on the host for the container build.

### GPU or CPU configuration

The Ollama service in `compose.yaml` contains:

```yaml
gpus: all
```

This requires [Docker Compose 2.30.0 or later](https://docs.docker.com/reference/compose-file/services/#gpus), compatible hardware and configured GPU support for containers. For NVIDIA GPUs, follow the [Ollama Docker instructions](https://docs.ollama.com/docker), including NVIDIA Container Toolkit setup where required.

For CPU-only execution, remove `gpus: all` from the Ollama service before running Compose. Ollama supports CPU-only containers, but inference may exceed Agent's configured 60-second model timeout. That timeout is set in `agent-service/src/main/resources/application.properties` as `langchain4j.ollama.chat-model.timeout`; rebuild Agent after changing it for container execution.

## Build and run with Docker Compose

### 1. Clone the repository

```sh
git clone https://github.com/lafexx/DispatchAI.git
cd DispatchAI
```

SSH users can instead clone `git@github.com:lafexx/DispatchAI.git`.

### 2. Validate the environment

Start Docker, select the GPU or CPU configuration above, then run:

```sh
docker version
docker compose version
docker compose config --quiet
python3 --version
```

Resolve configuration errors and port conflicts before continuing. No hosted LLM API key or mandatory `.env` file is needed for the supplied deployment.

### 3. Start Ollama and download the model

```sh
docker compose up -d --wait ollama
docker compose exec ollama ollama pull llama3.1:8b
docker compose exec ollama ollama list
```

Confirm that the model list includes `llama3.1:8b`. **The pull command is required on first use: the warm-up job does not download a missing model.** The `ollama-data` volume retains the download between starts, so repeat the pull only when the model is absent or you intend to update it.

### 4. Build and start the application

```sh
docker compose up -d --build
docker compose ps -a
```

The Java Dockerfiles compile and package the services using JDK 21 and their Maven wrappers. They pass `-DskipTests`; run the checks in [Testing](#testing) separately.

The `broker-storage-init` job prepares the broker volume and should exit with code 0. Kafka must become healthy before its dependent services start. `ollama-warmup` sends an initial model request and should also complete successfully before Agent starts. These are one-off jobs, so they do not need to remain running. A started application container can still be initialising; check its API before seeding.

### 5. Check readiness

```sh
docker compose logs --tail=100 broker delivery-service fleet-service dispatch-service customer-service analytics-service agent-service ollama-warmup

curl --fail-with-body http://localhost:8084/api/customers
curl --fail-with-body http://localhost:8082/api/drivers
curl --fail-with-body http://localhost:8082/api/vehicles
curl --fail-with-body http://localhost:8085/api/analytics/deliveries/summary
curl --fail-with-body 'http://localhost:8085/api/analytics/deliveries/throughput?minutes=5'
```

Empty resource lists are valid before seeding. Analytics may temporarily return HTTP 503 while its Kafka Streams stores initialise or restore; wait and retry read requests.

Open [http://localhost:3000](http://localhost:3000) once the APIs are available. These checks do not exercise model inference; use the [Agent examples](#agent-examples) to check that separately.

## Service addresses

| Component           | Host address                                     | Responsibility                              |
| ------------------- | ------------------------------------------------ | ------------------------------------------- |
| Frontend            | [http://localhost:3000](http://localhost:3000)   | Desktop operations interface                |
| Delivery            | [http://localhost:8081](http://localhost:8081)   | Delivery lifecycle and accepted assignments |
| Fleet               | [http://localhost:8082](http://localhost:8082)   | Drivers, vehicles and resource reservations |
| Dispatch            | [http://localhost:8083](http://localhost:8083)   | Eligibility and assignment coordination     |
| Customer            | [http://localhost:8084](http://localhost:8084)   | Customer records and notification history   |
| Analytics           | [http://localhost:8085](http://localhost:8085)   | Delivery projections and streaming queries  |
| Agent               | [http://localhost:8086](http://localhost:8086)   | Operational questions and dispatch advice   |
| Kafka host listener | `localhost:9092`                                 | Host-side Kafka clients                     |
| Ollama              | [http://localhost:11434](http://localhost:11434) | Local model API                             |

These ports must be free. Within Compose, applications use service DNS names, such as `http://fleet-service:8082`, and Kafka's internal listener, `broker:19092`. Host processes use `localhost` addresses instead.

## Demo data

The standard-library Python seeder uses the public REST APIs to create customers, drivers, vehicles and deliveries, then exercises assignment and delivery lifecycle operations. It does not write to databases directly, inject Kafka events or call Agent/Ollama.

Inspect the planned scenario without requests or file writes:

```sh
python3 scripts/seed-demo.py --dry-run
```

Populate the default scenario after the services are ready:

```sh
python3 scripts/seed-demo.py
```

| Record or outcome                  | Default count                             |
| ---------------------------------- | ----------------------------------------- |
| Customers                          | 4                                         |
| Drivers                            | 12                                        |
| Vehicles                           | 12                                        |
| Completed deliveries               | 9                                         |
| Cancelled deliveries               | 3                                         |
| Assigned deliveries                | 3                                         |
| In-transit deliveries              | 3                                         |
| Pending deliveries                 | 6                                         |
| Deliveries with recorded delays    | 6, included in the lifecycle counts above |
| Spare resources after the scenario | 6 drivers and 6 vehicles                  |

There are 24 deliveries in total. These are the scenario's intended additions; existing data can make the application's overall totals larger.

Wait for `Demo ready` before using the scenario. The script waits for accepted assignments, terminal resource release and matching analytics projections. Its `demo-seed-<run-id>.json` journal records real IDs, pending deliveries, available resources and request outcomes.

For several minutes of visible chart activity, use:

```sh
python3 scripts/seed-demo.py --interval 12 --timeout 180
```

The script uses current event timestamps and does not backdate events. `--timeout` controls each asynchronous/readiness wait, not the entire run. Charts can update at minute boundaries and recent windows can change as late events arrive.

Each run **appends** a new scenario. It does not reset data or automatically resume an interrupted run. POST requests are not automatically retried because a timed-out request may already have committed. Inspect the journal and application state before starting another run after a failure.

See [scripts/README.md](scripts/README.md) for custom counts, URL overrides, journal details and additional options.

### Suggested desktop walkthrough

1. Open Fleet and inspect the ready drivers and vehicles, including capacity.
2. Select a pending delivery and check its parcel weight.
3. Open Dispatch and select an eligible driver and vehicle, or request optional AI advice.
4. Confirm the assignment and wait for **Delivery** to report `ASSIGNED`. Dispatch's initial response alone does not establish Delivery acceptance.
5. Start the delivery, report a delay and complete it. Check that its resources are released.
6. Open Analytics to inspect throughput, delay history and lifetime statistics.

Newly created drivers start unavailable until marked ready. Vehicles also need readiness and sufficient capacity; the frontend's vehicle creation form can mark a new vehicle available immediately. Readiness and occupancy both affect eligibility.

## Agent examples

These commands make real model requests and may take longer than ordinary API reads. Run them after downloading the model and preparing data.

Ask an operational question:

```sh
curl --fail-with-body http://localhost:8086/api/agent/operations/query \
  -H 'Content-Type: application/json' \
  -d '{"message":"How many deliveries have recorded delays?"}'
```

Request advice using a real pending delivery ID from the seed journal or frontend. Replace the placeholder first:

```sh
DELIVERY_ID='replace-with-a-pending-delivery-uuid'

curl --fail-with-body http://localhost:8086/api/agent/dispatch/advice \
  -H 'Content-Type: application/json' \
  -d "{\"deliveryId\":\"$DELIVERY_ID\"}"
```

Advice does not assign resources. Confirm the proposal through Dispatch or the frontend; current eligibility and reservation checks still apply. Outputs depend on current data and the model, so these examples specify requests rather than fixed responses.

## Optional continuous simulator

The simulator is separate from the one-off seeder. It creates and progresses activity through service APIs without calling an LLM. Start the main stack first, then enable the additional Compose file and `demo` profile:

```sh
docker compose -f compose.yaml -f compose.simulator.yaml --profile demo up -d --build simulator
docker compose -f compose.yaml -f compose.simulator.yaml --profile demo logs -f simulator
```

Stop it before preparing a controlled demo:

```sh
docker compose -f compose.yaml -f compose.simulator.yaml --profile demo stop simulator
```

Configuration such as `SIM_SPEED`, `SIM_ARRIVAL_SECONDS` and `SIM_MAX_DELIVERIES` is defined in `compose.simulator.yaml`. The default delivery creation limit is 120, so this is bounded demo activity. Its state is retained in the `simulator-state` volume; keep that state consistent with the backend data.

## Testing

Run Java checks on a host with JDK 21 selected through `JAVA_HOME` and `PATH`. Maven itself does not need to be installed because each service provides a wrapper.

```sh
java -version

(
  for service in delivery-service fleet-service dispatch-service customer-service analytics-service agent-service
  do
    (cd "$service" && sh ./mvnw test) || exit 1
  done
)
```

Run the Python tests and service structure audit from the root:

```sh
python3 -m unittest discover -s scripts/tests -v
python3 -m unittest discover -s simulator/tests -v
python3 scripts/check-service-structure.py
```

Run frontend static checks and its production build with a supported Node.js version:

```sh
(cd frontend && npm ci && npm run lint && npm run build)
```

See [frontend/README.md](frontend/README.md) for the optional Playwright desktop smoke test. Fixture-based checks and source audits do not replace live acceptance testing of the Spring, Kafka and Ollama deployment.

## Local development outside containers

To run Java services locally, start only their containerised infrastructure and download the model as above:

```sh
docker compose up -d --wait broker ollama
```

Run each service in a **separate terminal**, starting Delivery, Fleet and Customer first, then Dispatch, Analytics and Agent. The following commands each assume the repository root:

```sh
(cd delivery-service && sh ./mvnw spring-boot:run)
(cd fleet-service && sh ./mvnw spring-boot:run)
(cd customer-service && sh ./mvnw spring-boot:run)
(cd dispatch-service && sh ./mvnw spring-boot:run)
(cd analytics-service && sh ./mvnw spring-boot:run)
(cd agent-service && sh ./mvnw spring-boot:run)
```

Each service's default configuration targets the host's `localhost` addresses. Do not run a local service alongside its Compose equivalent on the same port. Stop existing application containers first if switching from the full container deployment.

For local frontend development, stop the Compose frontend if it is using port 3000, then run:

```sh
(cd frontend && npm ci && npm run dev)
```

The Vite server proxies API requests to the backend services. Its local defaults target ports 8081–8086 on `localhost`, so it can also be used with containerised backend services. See [frontend/README.md](frontend/README.md) for proxy settings and deployment details.

## Configuration

Defaults are provided in `compose.yaml` and each service's `src/main/resources/application.properties`.

| Setting                                                                                      | Used by               | Meaning                                                                          |
| -------------------------------------------------------------------------------------------- | --------------------- | -------------------------------------------------------------------------------- |
| `KAFKA_BOOTSTRAP_SERVERS`                                                                    | Messaging services    | Kafka connection; `broker:19092` in Compose, `localhost:9092` for host execution |
| `FLEET_SERVICE_BASE_URL`                                                                     | Delivery and Dispatch | Fleet REST address                                                               |
| `DELIVERY_SERVICE_BASE_URL`                                                                  | Dispatch              | Delivery REST address                                                            |
| `ANALYTICS_SERVICE_URL`, `DELIVERY_SERVICE_URL`, `FLEET_SERVICE_URL`, `DISPATCH_SERVICE_URL` | Agent                 | Operational REST dependencies                                                    |
| `OLLAMA_BASE_URL`                                                                            | Agent                 | Ollama API; `http://ollama:11434` in Compose                                     |
| `CUSTOMER_SERVICE_URL` and the other five `*_SERVICE_URL` entries under `frontend`           | Frontend proxy        | Backend addresses used by the Vite server                                        |
| `FLEET_RESERVATION_LEASE_SECONDS`                                                            | Fleet                 | Provisional reservation lease, default 120 seconds                               |

For example, set the lease before startup:

```sh
FLEET_RESERVATION_LEASE_SECONDS=180 docker compose up -d fleet-service
```

The lease variable is explicitly interpolated by Compose. For other container settings, edit the relevant `environment` entry in Compose or use a Compose override file; exporting a host variable alone does not replace a hard-coded container environment value. For host execution, set the service's environment variables directly. Seeder URL overrides are documented separately in [scripts/README.md](scripts/README.md).

## Shutdown and persistence

For the base stack:

```sh
docker compose down
```

If the simulator was enabled, use the combined configuration to remove its container too:

```sh
docker compose -f compose.yaml -f compose.simulator.yaml --profile demo down
```

These commands retain named volumes. Delivery, Fleet, Dispatch and Customer persist H2 files in their own volumes; Kafka retains broker data and Ollama retains downloaded models. Analytics derives its state from Kafka and may need time to restore after container recreation.

Avoid `docker compose down -v` for ordinary restarts: it deletes named volumes, including application records, broker data and downloaded models. The project uses Hibernate schema updates for local development rather than managed production migrations.

## Troubleshooting

| Symptom                                              | Check or action                                                                                                                           |
| ---------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| Compose rejects `gpus` or cannot access a GPU        | Check Compose version and host container support, or remove `gpus: all` for CPU execution                                                 |
| Agent reports a missing model                        | Run `docker compose exec ollama ollama list`, then pull `llama3.1:8b` if absent                                                           |
| Model calls time out                                 | Check `docker compose logs --tail=100 ollama agent-service`, available resources and the configured model timeout                         |
| Kafka fails or applications cannot connect           | Inspect `docker compose logs --tail=100 broker-storage-init broker`; use `broker:19092` inside Compose and `localhost:9092` from the host |
| Frontend opens but API requests fail                 | Check service logs and the readiness requests above; frontend availability alone does not establish backend readiness                     |
| Analytics returns 503 or a new chart point is absent | Allow store restoration and minute boundaries; keep original event timestamps and projection lag in mind                                  |
| No eligible resources                                | Check pending delivery state, positive parcel weight, driver readiness, vehicle capacity and existing reservations                        |
| Seeder stops partway through                         | Inspect its JSON journal and actual service state before starting another run; uncertain POSTs may have committed                         |

After correcting a missing model, rerun warm-up and start its dependent services:

```sh
docker compose up -d --force-recreate ollama-warmup
docker compose up -d agent-service frontend
```

Inspect the warm-up logs and test an Agent request rather than relying only on container exit status.
