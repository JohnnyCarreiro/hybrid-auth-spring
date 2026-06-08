---
id: FEAT-013
slug: resource-projects-crud
epic: 003-resource
milestone: 003-resource
sdd: SDD-002
frd: F3
status: in progress
depends-on: [011-resource-jwt-verifier, 012-resource-user-mirror]
blocks: [014-resource-tasks-crud]
date: 2026-06-08
---

# FEAT-013 — resource projects CRUD (F3)

Realizes [[../02-epics/003-resource|EPIC-003 — resource]] · [[../01-milestones/003-resource|MILESTONE-003]].
Build of **[[../../architecture/sdds/sdd-tasks|SDD-002]] §8 F3** — Projects CRUD (owner-scoped). Acceptance
+ tasks are the SDD's.

## Intent

Owner-scoped CRUD over `/projects`. Plain Spring MVC — the auth is the interesting part. Another user's
project is invisible: **404, not 403** (the API never confirms others' resources — SDD-002 §4 invariant 5).

## Sequence

- **Depends on:** F1 (auth), F2 (the `owner_id` FK target).
- **Blocks:** F4 tasks CRUD (the parent project).

## Done when

SDD-002 §8 F3 acceptance met (owner-scoped CRUD; cross-user → 404; list shows only owned; blank name →
400; no token → 401) + feature DoD (happy + negative tests, CI green, PR).

## Build (2026-06-08)

**What landed.** `domain/project/Project` (born-consistent, owns its UUID v7, immutable `ownerId`) +
`ProjectNotFoundException`; `ProjectRepository` with **only** owner-scoped finders
(`findByIdAndOwnerId`, `findByOwnerIdOrderByCreatedAtDesc`, `existsByIdAndOwnerId` — no plain `findById`
in the service path); `ProjectService` (ownership → 404); `ProjectController` + `ProjectRequest`/`Response`;
`web/ResourceExceptionHandler` (RFC 7807, `code` property); migration `V3__projects.sql`.

**Tests.** `ProjectCrudIntegrationTest` on Testcontainers Postgres: full CRUD; cross-user get/update/delete
→ 404 `PROJECT_NOT_FOUND` + empty cross-user list; unauthenticated → 401; expired token → 401; blank name
→ 400. Green.

**Status.** Built and green; **pending human validation (§16.3)**.
