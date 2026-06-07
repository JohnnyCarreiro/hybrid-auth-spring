---
id: FEAT-010
slug: auth-signout
epic: 002-auth
milestone: 002-auth
sdd: SDD-001
frd: F6
status: planned
depends-on: [007-auth-signin, 009-auth-refresh-rotation]
blocks: []
date: 2026-06-07
---

# FEAT-010 — sign-out (session revocation)

Realizes [[../02-epics/002-auth|EPIC-002 — auth]] · [[../01-milestones/002-auth|MILESTONE-002]].
Build of **[[../../architecture/sdds/sdd-auth|SDD-001]] §8 F6** — Sign-out. Acceptance + tasks are the SDD's.

## Intent

Revoke the presented session (`revoked_at` set); a subsequent rotate on it → 401. The access token lives
out its ≤15 min TTL (accepted gap).

## Sequence

- **Depends on:** F3 sign-in (a session to revoke); reuses F5's `revokeFamily` / revocation path.
- **Blocks:** —
- Last feature of the epic; closes EPIC-002 → triggers the `v0.2.0` release.

## Done when

SDD-001 §8 F6 acceptance met (sign-out → `revoked_at` set; later rotate → 401) + feature DoD (integration
test, CI green, PR into `epic/002-auth`). On close: epic `exits_with` checked → `dev → main` → `v0.2.0`.
