---
id: MILESTONE-001
slug: bootstrap
title: Bootstrap
window: 2026-06
status: done
date: 2026-06-05
---

# MILESTONE-001 — Bootstrap

1:1 with EPIC-001.

## Goal

Turn the documented design into a running skeleton: a Gradle multi-module monorepo with both Spring
Boot services, Postgres + Redis via docker compose, and CI — reproducible from a clean checkout with
`./gradlew build` and `docker compose up`. No domain logic yet.

## Delivers (SDDs / epics it groups)

- EPIC-001 — bootstrap (cross-cutting; see below).

## Cross-cutting items (no SDD)

- EPIC-001 — bootstrap: build, compose, CI, lint gate.

## Exit

- [x] `./gradlew build` green across modules.
- [x] `docker compose up` brings up both services + Postgres + Redis.
- [x] CI runs build + test on push/PR (OQ-002).
- [x] Lint/format gate wired (OQ-003).
- [x] Released: `dev → main` merge tagged `v0.1.0` (playbook §16.4 / §5).
