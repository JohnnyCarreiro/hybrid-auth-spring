---
id: EPIC-001
slug: bootstrap
type: infra
status: planned
owner: Johnny Carreiro
sdd:
target_window: 2026-06
milestone: 001-bootstrap
roadmap_cards:
  - 001-build-skeleton
  - 002-ci-pipeline
sprints: []
related_decisions:
  - ADR-0001 — testing stack
exits_with:
  - gradlew build green
  - docker compose up brings the stack up
  - CI build + test on push/PR
  - lint/format gate wired
  - released v0.1.0
---

# EPIC-001 — bootstrap

Milestone: [[../01-milestones/001-bootstrap|MILESTONE-001]] (1:1). Features: [[../03-features/001-build-skeleton|FEAT-001]], [[../03-features/002-ci-pipeline|FEAT-002]].

## Why

The design is documented; now it needs a running skeleton so feature work has somewhere to land. A
Gradle multi-module monorepo with both Spring Boot services, Postgres + Redis via docker compose, and
CI — reproducible from a clean checkout.

## Outcome

`./gradlew build` is green across modules; `docker compose up` brings up both services + Postgres +
Redis; CI runs build + test on every push/PR; the first release is tagged `v0.1.0`.

## Scope (in)

- Gradle multi-module: `auth-service`, `resource-service`, optional `shared`.
- Spring Boot 3.5 app skeletons (health endpoints, dev profile).
- docker-compose: Postgres + Redis + both services.
- CI workflow (build + test) and a lint/format gate.

## Out of scope

- Domain logic — auth flows (F1–F6, see SDD-001 §8) and projects/tasks → capability epics.

## Exits with

- [ ] `./gradlew build` green across modules.
- [ ] `docker compose up` brings the full stack up.
- [ ] CI runs build + test on push/PR (OQ-002).
- [ ] Lint/format gate wired (OQ-003).
- [ ] Released: `dev → main` merge tagged `v0.1.0`.

## Related decisions

- ADR-0001 — testing stack (Testcontainers needs Docker available in CI).

## Risks / open questions

- OQ-001 (author `playbook-java.md`), OQ-002 (Java CI + Docker for Testcontainers), OQ-003
  (Spotless/google-java-format), OQ-005 (UUID v7 generator) — all land during this epic.

## Progress log

2026-06-05 — Planned.
