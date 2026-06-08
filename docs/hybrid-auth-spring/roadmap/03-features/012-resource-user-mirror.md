---
id: FEAT-012
slug: resource-user-mirror
epic: 003-resource
milestone: 003-resource
sdd: SDD-002
frd: F2
status: in progress
depends-on: [011-resource-jwt-verifier]
blocks: [013-resource-projects-crud]
date: 2026-06-08
---

# FEAT-012 — resource identity mirror (F2)

Realizes [[../02-epics/003-resource|EPIC-003 — resource]] · [[../01-milestones/003-resource|MILESTONE-003]].
Build of **[[../../architecture/sdds/sdd-tasks|SDD-002]] §8 F2** — Identity mirror (create-only JIT sync).
Acceptance + tasks are the SDD's. Decision: [[../../architecture/adrs/0006-user-identity-sync|ADR-0006]].

## Intent

Keep a local `app.users` row per auth user (keyed by the token `sub`) so the app domain can own a real
`projects.owner_id` FK without a cross-DB reference (ADR-0003). Provision JIT on the first authenticated
request from the token claims; **create-only** — never updated from a later token. The update path
(auth-emitted event preferred; outbound callback the lesser alternative) is documented and **deferred**
(F-sync / OQ-007).

## Sequence

- **Depends on:** F1 (a verified `sub` to mirror).
- **Blocks:** F3 projects CRUD (the `owner_id` FK target).

## Done when

SDD-002 §8 F2 acceptance met (first request inserts one row from claims; a later changed-email token does
not overwrite; concurrent first-requests → one row) + feature DoD (integration test, CI green, PR).

## Build (2026-06-08)

**What landed.** `domain/identity/AppUser` (mirror entity; id = auth `sub`, **not** minted here);
`AppUserRepository.provisionIfAbsent` (`INSERT … ON CONFLICT (id) DO NOTHING` — create-only,
concurrency-safe); `services/UserMirror`; `infra/web/MirrorSyncInterceptor` + `WebConfig` (provision from
the verified claims before the controller runs, so the FK is satisfiable; excluded from `/health`).

**Tests.** `UserMirrorIntegrationTest`: first authenticated request provisions exactly one row from the
claims; a later token with a changed email does **not** overwrite it (create-only). Green.

**Status.** Built and green; **pending human validation (§16.3)**. F-sync (update propagation) deferred — OQ-007.
