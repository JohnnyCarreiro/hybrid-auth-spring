---
id: FEAT-003
slug: runtime-baseline
epic: 001-bootstrap
milestone: 001-bootstrap
sdd:
frd:
status: planned
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

- [ ] Both services run on **Jetty** (no `tomcat` on the classpath); `/health` still `{"status":"UP"}`.
- [ ] Datasource per service: auth-service → `auth` DB (`auth_user`), resource-service → `app` DB (`app_user`); connection from env (localhost in dev, `postgres` in compose).
- [ ] **Flyway runs on startup** per service against its own DB; the baseline migration applies; `flyway_schema_history` exists in **both** `auth` and `app`.
- [ ] `docker compose up` brings the stack up with migrations applied; `./gradlew build` green.

## Tasks

- [ ] Swap Tomcat → Jetty in both modules (exclude `spring-boot-starter-tomcat`, add `spring-boot-starter-jetty`).
- [ ] Add `spring-boot-starter-data-jpa` + `postgresql` driver + `flyway-core` + `flyway-database-postgresql` to both.
- [ ] Datasource config per service (env: `*_DATASOURCE_URL`/user/pass) → its isolated DB.
- [ ] Flyway config + baseline migration `V1__baseline.sql` under each module's `db/migration` (no domain tables yet).
- [ ] Compose: pass DB env to each service; confirm migrations run on `docker compose up`.
- [ ] Task runner: `dev-*` now need infra — make `dev-run`/`dev-auth`/`dev-resource` bring up `docker-up-infra` first (or document it).
- [ ] Verify: `docker compose up` → both `/health` UP, `flyway_schema_history` present in `auth` and `app` (`docker-psql-auth` / `docker-psql-app`).

## Out of scope

- Domain tables/migrations (`users`/`sessions`/`jwks`, `projects`/`tasks`) — land with their own features.

## Notes

- After this lands, the apps **require Postgres to boot** (datasource autoconfig) — the skeleton's
  "boots without infra" property ends here, by design.
- Per-service migrations keep the two systems isolated end-to-end (ADR-0003): each owns its schema history.
