# QueueForge — Distributed Job Processing Platform

[![CI](https://github.com/Preethi-23102000/QueueForge---Distributed-Job-Processing-Platform/actions/workflows/ci.yml/badge.svg)](https://github.com/Preethi-23102000/QueueForge---Distributed-Job-Processing-Platform/actions/workflows/ci.yml)

A distributed background-job processing platform. Clients submit long-running jobs through a REST API; jobs are queued in RabbitMQ; separate worker services process them asynchronously; and job state, retries, and execution history are persisted in PostgreSQL.

> **Built in public, stage by stage.** See **Project Status** below for what currently works.

## Project Status

| Stage | Scope | Status |
|-------|-------|--------|
| **1 — Core Platform** | API + RabbitMQ + PostgreSQL + Worker, job states, execution history, Docker Compose | ✅ Complete |
| **2 — Reliability & Testing** | Automatic retry with backoff, dead-letter queue, idempotent submission, concurrency safety, Testcontainers, GitHub Actions CI | ✅ Complete |
| 3 — Observability & Operations | Prometheus, Grafana, structured logs, health checks, metrics | ⬜ Not started |
| 4 — Deployment & Scale | Kubernetes, multiple worker replicas, load testing, benchmarks | ⬜ Not started |

## Reliability (Stage 2)

- **Automatic retries with exponential backoff** — a failed job is re-queued via a retry queue with a growing delay (`base × 2^attempt`), moving through a `RETRYING` state.
- **Dead-letter queue** — jobs that exhaust their retries are marked `DEAD_LETTERED` and routed to a DLQ for inspection.
- **Idempotent submission** — an optional `Idempotency-Key` header de-duplicates jobs so a retried request never creates a duplicate.
- **Concurrency safety** — optimistic locking (a `@Version` column) prevents two workers from corrupting the same job.
- **Tested** — JUnit unit tests plus Testcontainers integration tests (real Postgres + RabbitMQ) run in CI on every push and pull request.

## Architecture

```
Client
  |
  v
Spring Boot API ---> PostgreSQL (job metadata, state, history)
  |
  +--> RabbitMQ ---> Worker Service ---> PostgreSQL (status updates)
```

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 4.1 |
| API | Spring Web |
| Messaging | RabbitMQ 4.3 |
| Database | PostgreSQL 18 |
| Migrations | Flyway |
| API docs | springdoc-openapi (Swagger UI) |
| Build | Maven (multi-module) |
| Containers | Docker / Docker Compose |

## Module Layout

```
queueforge-common   shared enums, DTOs, message schema, JPA entities, repositories
queueforge-api      REST API: accepts jobs, persists metadata, publishes to RabbitMQ
queueforge-worker   consumes jobs from RabbitMQ, processes them, updates state
```

## Job Lifecycle

```
QUEUED -> PROCESSING -> COMPLETED
QUEUED -> PROCESSING -> FAILED (-> QUEUED on retry)
QUEUED -> CANCELLED
```

## REST API

| Method | Path | Purpose |
|--------|------|---------|
| POST   | `/api/v1/jobs` | Submit a job (returns `202 Accepted`) |
| GET    | `/api/v1/jobs/{id}` | Get a job by id |
| GET    | `/api/v1/jobs` | List jobs (paged; `status`, `jobType` filters) |
| GET    | `/api/v1/jobs/{id}/executions` | Get execution history for a job |
| POST   | `/api/v1/jobs/{id}/retry` | Retry a failed job |
| POST   | `/api/v1/jobs/{id}/cancel` | Cancel a queued job |
| GET    | `/actuator/health` | Health check |

Full request/response examples and a field reference: [docs/api-examples.md](docs/api-examples.md).
Swagger UI (when running): `http://localhost:8080/swagger-ui.html`.

### Submit request fields (`POST /api/v1/jobs`)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `jobType` | enum | yes | `REPORT_GENERATION` or `CSV_VALIDATION` |
| `payload` | object | no | Free-form JSON passed to the worker (see flags below) |
| `maxRetries` | integer | no | Max retries, `>= 0`, default `3` |

Optional `payload` flags for the simulated worker:

| Flag | Type | Effect |
|------|------|--------|
| `sleepMs` | integer | How long the job runs before finishing (cap 300000). Omit for random 2–5 s. |
| `simulateFailure` | boolean | If `true`, the job fails after running. |

### Sample request bodies

```json
// Normal job
{ "jobType": "REPORT_GENERATION", "payload": { "reportName": "monthly-sales" } }

// Long-running job (30s) — watch it sit in PROCESSING
{ "jobType": "REPORT_GENERATION", "payload": { "sleepMs": 30000 } }

// Failing job — exercises the failure path
{ "jobType": "REPORT_GENERATION", "payload": { "simulateFailure": true } }

// Runs 10s then fails
{ "jobType": "REPORT_GENERATION", "payload": { "sleepMs": 10000, "simulateFailure": true } }
```

## Getting Started

> Requires Java 17, Maven, and Docker Desktop.

### Run the full stack with Docker Compose

```bash
docker compose up --build
```

This starts PostgreSQL, RabbitMQ, the API (port 8080), and the worker. Submit a
job with the examples above and watch it move from `QUEUED` to `COMPLETED`.

RabbitMQ management UI: `http://localhost:15672` (user/pass: `queueforge`).

### Try it end-to-end

```bash
# 1) submit a job -> returns a jobId
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"jobType":"REPORT_GENERATION","payload":{"reportName":"demo"}}'

# 2) check it (after ~5s it becomes COMPLETED)
curl http://localhost:8080/api/v1/jobs/<jobId>

# 3) see its execution history
curl http://localhost:8080/api/v1/jobs/<jobId>/executions
```

### Build only

```bash
mvn clean install
```

## Roadmap

This project is intentionally built in stages so each stage is independently usable.