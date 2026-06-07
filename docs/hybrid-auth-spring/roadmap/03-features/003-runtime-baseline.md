---
id: FEAT-003
slug: runtime-baseline
epic: 001-bootstrap
milestone: 001-bootstrap
sdd:
frd:
status: done
depends-on: [001-build-skeleton]
blocks: []
date: 2026-06-07
---

# FEAT-003 — runtime baseline

Realizes [[../02-epics/001-bootstrap|EPIC-001 — bootstrap]] · [[../01-milestones/001-bootstrap|MILESTONE-001]].
Infra feature — no FRD/SDD. Decisions: [[../../architecture/adrs/0004-runtime-baseline-jetty-flyway|ADR-0004]] (Jetty + Flyway), [[../../architecture/adrs/0003-database-per-service-isolation|ADR-0003]] (two DBs).

## Intent

Make the runtime production-shaped before any domain code: swap the embedded server to **Jetty**, wire
each service to **its own isolated database**, and stand up the **Flyway migration pipeline** (per
service) with a baseline. No domain tables yet.

## Acceptance

- [x] Both services run on **Jetty** (no `tomcat` on the classpath); `/health` still `{"status":"UP"}`.
- [x] Datasource per service: auth-service → `auth` DB (`auth_user`), resource-service → `app` DB (`app_user`); connection from env (localhost in dev, `postgres` in compose).
- [x] **Flyway runs on startup** per service against its own DB; the baseline migration applies; `flyway_schema_history` exists in **both** `auth` and `app`.
- [x] `docker compose up` brings the stack up with migrations applied; `./gradlew build` green.

## Tasks

- [x] Swap Tomcat → Jetty in both modules (exclude `spring-boot-starter-tomcat`, add `spring-boot-starter-jetty`).
- [x] Add `spring-boot-starter-data-jpa` + `postgresql` driver + `flyway-core` + `flyway-database-postgresql` to both.
- [x] Datasource config per service (env: `*_DATASOURCE_URL`/user/pass) → its isolated DB.
- [x] Flyway config + baseline migration `V1__baseline.sql` under each module's `db/migration` (no domain tables yet).
- [x] Compose: pass DB env to each service; confirm migrations run on `docker compose up`.
- [x] Task runner: `dev-*` now need infra — make `dev-run`/`dev-auth`/`dev-resource` bring up `docker-up-infra` first (or document it).
- [x] Verify: `docker compose up` → both `/health` UP, `flyway_schema_history` present in `auth` and `app` (`docker-psql-auth` / `docker-psql-app`).

## Out of scope

- Domain tables/migrations (`users`/`sessions`/`jwks`, `projects`/`tasks`) — land with their own features.

## Notes

- After this lands, the apps **require Postgres to boot** (datasource autoconfig) — the skeleton's
  "boots without infra" property ends here, by design.
- Per-service migrations keep the two systems isolated end-to-end (ADR-0003): each owns its schema history.
- **Build tooling:** dropped the BuildKit `--mount=type=cache` from the Dockerfiles (local Docker has no
  buildx) — documented as a CI tip in the Dockerfiles. `docker-build` = `gradle assemble` (no tests); the
  full suite (Testcontainers) runs on the host/CI via `test` / `./gradlew test`, since Testcontainers needs
  a native Docker env that nested dockerized-gradle doesn't give.

## Retro

**Shipped:** Jetty 12.0.21 replacing Tomcat · per-service datasources to the isolated `auth`/`app` DBs ·
Flyway pipeline + baseline · `test` (host) recipe + `dev-*` gated on `docker-up-infra`.
**Verified (compose up):** both `/health` UP, auth healthy *before* resource starts, `flyway_schema_history`
V1 `baseline` success in **both** `auth` and `app`.
**Punted:** domain tables/migrations → their features; buildx-based CI build cache → FEAT-002 (ci).
