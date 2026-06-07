---
id: FEAT-008
slug: auth-me
epic: 002-auth
milestone: 002-auth
sdd: SDD-001
frd: F4
status: planned
depends-on: [007-auth-signin, 006-auth-jwks]
blocks: []
date: 2026-06-07
---

# FEAT-008 — current user (`/me`)

Realizes [[../02-epics/002-auth|EPIC-002 — auth]] · [[../01-milestones/002-auth|MILESTONE-002]].
Build of **[[../../architecture/sdds/sdd-auth|SDD-001]] §8 F4** — `/me`. Acceptance + tasks are the SDD's.

## Intent

Return the authenticated user for a valid access JWT — the auth-service's security filter chain protecting
a route. Closes the issue→verify loop with the smallest protected endpoint before the rotation deep-dive.

## Sequence

- **Depends on:** F3 sign-in (a JWT to present) + F2 JWKS/keys (keys to verify it).
- **Blocks:** —

## Done when

SDD-001 §8 F4 acceptance met (valid Bearer → 200 user; missing/invalid/expired → 401) + feature DoD
(integration test of valid + 401 paths, CI green, PR into `epic/002-auth`).
