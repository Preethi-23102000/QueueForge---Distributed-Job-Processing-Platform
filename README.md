# QueueForge — Distributed Job Processing Platform

A distributed background-job processing platform. Clients submit long-running jobs through a REST API; jobs are queued in RabbitMQ; separate worker services process them asynchronously; and job state, retries, and execution history are persisted in PostgreSQL.

> 🚧 **Built in public, stage by stage.** See **Project Status** below for what currently works.

## Project Status

| Stage | Scope | Status |
|-------|-------|--------|
| **1 — Core Platform** | API + RabbitMQ + PostgreSQL + Worker, job states, execution history, Docker Compose | 🟡 In progress |
| 2 — Reliability & Testing | Retries, dead-letter queue, idempotency, concurrency safety, Testcontainers, CI | ⬜ Not started |
| 3 — Observability & Operations | Prometheus, Grafana, structured logs, health checks, metrics | ⬜ Not started |
| 4 — Deployment & Scale | Kubernetes, multiple worker replicas, load testing, benchmarks | ⬜ Not started |

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

## Getting Started

> Requires Java 17, Maven, and Docker Desktop.

```
# Build all modules
mvn clean install

# (later) start the full stack
docker compose up --build
```

## Roadmap

This project is intentionally built in stages so each stage is independently usable. Details and API examples will be added as features land.