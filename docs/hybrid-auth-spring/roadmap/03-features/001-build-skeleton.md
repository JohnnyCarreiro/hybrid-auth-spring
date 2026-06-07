---
id: FEAT-001
slug: build-skeleton
epic: 001-bootstrap
milestone: 001-bootstrap
sdd:
frd:
status: in-progress
depends-on: []
blocks: [002-ci-pipeline]
date: 2026-06-06
---

# FEAT-001 — build skeleton

Realizes [[../02-epics/001-bootstrap|EPIC-001 — bootstrap]] · [[../01-milestones/001-bootstrap|MILESTONE-001]].
Infra feature — no FRD/SDD.

## Intent

A Gradle multi-module monorepo with both Spring Boot apps and local infra, reproducible from a clean
checkout. No domain logic.

## Acceptance

- [x] `settings.gradle.kts` includes `auth-service`, `resource-service`, `shared` (Kotlin DSL).
- [x] Both apps boot (`contextLoads` green) and `./gradlew build` is green across modules (verified via Dockerized `gradle:8.10.2-jdk21`).
- [x] `docker-compose.yml` wires Postgres (**isolated `auth` + `app` databases** via init script, ADR-0003) + Redis + both services (`docker compose config` valid). Full `compose up --build` smoke pending.

## Tasks

- [x] Root Gradle build + version catalog (`gradle/libs.versions.toml`); module skeletons (Boot 3.5, Java 21).
- [x] `auth-service` + `resource-service` app classes + `GET /health` (actuator remapped to root).
- [x] `docker-compose.yml` (Postgres + Redis + services) + dev `.env.example` + per-service Dockerfile.
- [x] Dev task runner: `Makefile` + `justfile` (mirrored; docker-prefixed recipes: `docker-build/test/clean`, `docker-up`/`docker-up-infra`, `docker-run` (both services) + `docker-run-auth`/`docker-run-resource`, `docker-down(-v)`, `docker-logs/ps`, `docker-psql-auth/app`; plus `health`, `fmt`/`check` stubs).
- [ ] Smoke: `docker compose up --build` then both `/health` return 200 (needs the 2 image builds).

## Notes

- Base package `com.johnnycarreiro.hybridauth`; ports `AUTH_PORT=3333` / `RESOURCE_PORT=3334` (env, per the backend convention).
- Domain deps (JPA/Redis/Security) intentionally **not** added yet — the skeleton boots on `web`+`actuator` only, so it stays green without infra. They land with the auth features.
