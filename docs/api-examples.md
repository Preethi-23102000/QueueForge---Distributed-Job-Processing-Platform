# QueueForge API Guide

Base URL (local): `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`
RabbitMQ UI: `http://localhost:15672` (user/pass: `queueforge`)

All request/response bodies are JSON. Send `Content-Type: application/json` on POSTs
(Postman sets this automatically when the body type is **raw → JSON**).

---

## Endpoint reference

| # | Method | Path | Purpose | Success status |
|---|--------|------|---------|----------------|
| 1 | POST | `/api/v1/jobs` | Submit a job | `202 Accepted` |
| 2 | GET | `/api/v1/jobs/{jobId}` | Get one job | `200 OK` |
| 3 | GET | `/api/v1/jobs` | List jobs (paged, filterable) | `200 OK` |
| 4 | GET | `/api/v1/jobs/{jobId}/executions` | Execution history for a job | `200 OK` |
| 5 | POST | `/api/v1/jobs/{jobId}/retry` | Re-queue a FAILED job | `200 OK` |
| 6 | POST | `/api/v1/jobs/{jobId}/cancel` | Cancel a QUEUED job | `200 OK` |

`{jobId}` is a path value — substitute the real UUID, e.g. `/api/v1/jobs/8792544e-...`.

---

## Submit request fields (`POST /api/v1/jobs`)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `jobType` | enum | yes | Kind of job. One of `REPORT_GENERATION`, `CSV_VALIDATION`. |
| `payload` | object | no | Free-form JSON passed to the worker. Supports the control flags below. |
| `maxRetries` | integer | no | Max retry attempts. Must be `>= 0`. Defaults to `3`. |

### Payload control flags (simulated worker)

These optional flags in `payload` shape how the simulated job behaves:

| Flag | Type | Effect |
|------|------|--------|
| `sleepMs` | integer | How long the job "works" before finishing. Capped at `300000` (5 min). Omit for a random 2–5 s. |
| `simulateFailure` | boolean | If `true`, the job throws after sleeping, producing a `FAILED` result. |

Any other keys in `payload` are stored and returned untouched.

---

## Job statuses

| Status | Meaning |
|--------|---------|
| `QUEUED` | Accepted, waiting for a worker |
| `PROCESSING` | A worker is running it |
| `COMPLETED` | Finished successfully (`result` populated) |
| `FAILED` | Threw an error (`errorMessage` populated) |
| `CANCELLED` | Cancelled while still queued |

---

## Sample request bodies

**Normal job**
```json
{
  "jobType": "REPORT_GENERATION",
  "payload": { "reportName": "monthly-sales" },
  "maxRetries": 3
}
```

**Long-running job (runs 30 seconds)** — good for watching the `PROCESSING` state
```json
{
  "jobType": "REPORT_GENERATION",
  "payload": { "sleepMs": 30000 }
}
```

**Failing job** — exercises the failure path
```json
{
  "jobType": "REPORT_GENERATION",
  "payload": { "simulateFailure": true }
}
```

**Runs 10 seconds, then fails** — combine both flags
```json
{
  "jobType": "REPORT_GENERATION",
  "payload": { "sleepMs": 10000, "simulateFailure": true }
}
```

---

## List query parameters (`GET /api/v1/jobs`)

| Param | Type | Description |
|-------|------|-------------|
| `status` | enum | Filter by status (e.g. `COMPLETED`, `FAILED`) |
| `jobType` | enum | Filter by type (e.g. `REPORT_GENERATION`) |
| `page` | integer | Page number, 0-based (default `0`) |
| `size` | integer | Page size (default `20`) |
| `sort` | string | Sort, e.g. `createdAt,desc` |

Examples:
```
GET /api/v1/jobs?page=0&size=20
GET /api/v1/jobs?status=COMPLETED
GET /api/v1/jobs?jobType=REPORT_GENERATION&sort=createdAt,desc
```

---

## Walkthrough (all six endpoints)

```bash
# 1) Submit a job -> returns 202 with a jobId (status QUEUED)
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"jobType":"REPORT_GENERATION","payload":{"reportName":"demo"}}'

# 2) Get the job (after ~5s it becomes COMPLETED)
curl http://localhost:8080/api/v1/jobs/<jobId>

# 3) List jobs
curl "http://localhost:8080/api/v1/jobs?size=20"

# 4) Execution history
curl http://localhost:8080/api/v1/jobs/<jobId>/executions

# 5) Retry a FAILED job (submit one with simulateFailure first)
curl -X POST http://localhost:8080/api/v1/jobs/<failedJobId>/retry

# 6) Cancel a QUEUED job (only works before a worker picks it up)
curl -X POST http://localhost:8080/api/v1/jobs/<queuedJobId>/cancel
```

> Tip for testing cancel: `docker compose stop worker`, submit a job (it stays QUEUED),
> cancel it, then `docker compose start worker`.

---

## Sample responses

**Submit (`202 Accepted`)**
```json
{
  "jobId": "8792544e-3dd4-4e46-b798-cafca0b821d7",
  "jobType": "REPORT_GENERATION",
  "status": "QUEUED",
  "payload": { "reportName": "demo" },
  "retryCount": 0,
  "maxRetries": 3,
  "result": null,
  "errorMessage": null,
  "createdAt": null,
  "updatedAt": null,
  "startedAt": null,
  "completedAt": null
}
```

**Get after completion (`200 OK`)**
```json
{
  "jobId": "8792544e-3dd4-4e46-b798-cafca0b821d7",
  "jobType": "REPORT_GENERATION",
  "status": "COMPLETED",
  "payload": { "reportName": "demo" },
  "retryCount": 0,
  "maxRetries": 3,
  "result": {
    "message": "Processed REPORT_GENERATION",
    "processedAt": "2026-07-15T03:38:03.790Z"
  },
  "errorMessage": null,
  "createdAt": "2026-07-15T03:37:59.245Z",
  "updatedAt": "2026-07-15T03:38:03.797Z",
  "startedAt": "2026-07-15T03:37:59.548Z",
  "completedAt": "2026-07-15T03:38:03.790Z"
}
```

**Execution history (`200 OK`)**
```json
[
  {
    "id": "b1a2...",
    "jobId": "8792544e-3dd4-4e46-b798-cafca0b821d7",
    "attemptNumber": 1,
    "status": "COMPLETED",
    "workerId": "worker-1a2b3c4d",
    "startedAt": "2026-07-15T03:37:59.548Z",
    "completedAt": "2026-07-15T03:38:03.790Z",
    "durationMs": 4242,
    "errorMessage": null
  }
]
```

**Error (`404` / `409` / `400`)**
```json
{
  "timestamp": "2026-07-15T03:41:53.983Z",
  "status": 409,
  "error": "Conflict",
  "message": "Only QUEUED jobs can be cancelled; job 3cc2a0f4-... is PROCESSING"
}
```
