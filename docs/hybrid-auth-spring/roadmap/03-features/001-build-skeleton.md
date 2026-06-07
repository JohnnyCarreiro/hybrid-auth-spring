---
id: FEAT-001
slug: build-skeleton
epic: 001-bootstrap
milestone: 001-bootstrap
sdd:
frd:
status: done
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
- [x] `docker-compose.yml` wires Postgres (**isolated `auth` + `app` databases** via init script, ADR-0003) + Redis + both services. Verified end-to-end: 4 containers up, resource-service gated on auth-service health, both `/health` → `{"status":"UP"}`, banners shown.

## Tasks

- [x] Root Gradle build + version catalog (`gradle/libs.versions.toml`); module skeletons (Boot 3.5, Java 21).
- [x] `auth-service` + `resource-service` app classes + `GET /health` (actuator remapped to root).
- [x] `docker-compose.yml` (Postgres + Redis + services) + dev `.env.example` + per-service Dockerfile.
- [x] Dev task runner: `Makefile` + `justfile` (mirrored; docker-prefixed recipes: `docker-build/test/clean`, `docker-up`/`docker-up-infra`, `docker-run` (both services) + `docker-run-auth`/`docker-run-resource`, `docker-down(-v)`, `docker-logs/ps`, `docker-psql-auth/app`; plus `health`, `fmt`/`check` stubs).
- [x] Smoke: `docker compose up` → 4 containers up, both `/health` 200, banners shown.
- [x] Startup banner in both services (`src/main/resources/banner.txt`).
- [x] Health-gated startup: resource-service waits for auth-service `service_healthy` (ADR-0002).
- [x] Fast host dev loop: `dev-run` / `dev-auth` / `dev-resource` (bootRun) + Spring DevTools (hot reload); Docker builds use a BuildKit cache mount.

## Notes

- Base package `com.johnnycarreiro.hybridauth`; ports `AUTH_PORT=3333` / `RESOURCE_PORT=3334` (env, per the backend convention).
- Domain deps (JPA/Redis/Security) intentionally **not** added yet — the skeleton boots on `web`+`actuator` only, so it stays green without infra. They land with the auth features (datasources + Flyway come in [[003-runtime-baseline|FEAT-003]]).

## Retro

**Shipped:** Gradle multi-module skeleton (Kotlin DSL) · 2 isolated databases (auth/app) · Makefile+justfile task runner · startup banner · health-gated startup order · fast bootRun dev loop + cached Docker builds.
**Punted:** embedded server swap, datasources + Flyway → FEAT-003 (runtime baseline); CI/lint → FEAT-002.
**Surprises:** `temurin:jre` has no curl → installed it for the healthcheck; Docker dev loop is slow (full image build) → added host bootRun for the inner loop.
