---
id: FEAT-001
slug: build-skeleton
epic: 001-bootstrap
milestone: 001-bootstrap
sdd:
frd:
status: planned
depends-on: []
blocks: [002-ci-pipeline]
date: 2026-06-06
---

# FEAT-001 — build skeleton

Realizes [[../epics/001-bootstrap|EPIC-001 — bootstrap]] · [[../milestones/001-bootstrap|MILESTONE-001]].
Infra feature — no FRD/SDD.

## Intent

A Gradle multi-module monorepo with both Spring Boot apps and local infra, reproducible from a clean
checkout. No domain logic.

## Acceptance

- [ ] `settings.gradle(.kts)` includes `auth-service`, `resource-service`, optional `shared`.
- [ ] Both apps boot (health endpoint, dev profile); `./gradlew build` green across modules.
- [ ] `docker compose up` brings up Postgres + Redis + both services.

## Tasks

- [ ] Root Gradle build + version catalog; module skeletons (Boot 3.5, Java 21).
- [ ] `auth-service` + `resource-service` app classes + `GET /health`.
- [ ] `docker-compose.yml` (Postgres + Redis + services) + dev `.env.example`.
- [ ] Smoke: `docker compose up` then both `/health` return 200.
