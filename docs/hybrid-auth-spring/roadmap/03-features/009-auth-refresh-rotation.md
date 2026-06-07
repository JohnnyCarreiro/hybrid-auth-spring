---
id: FEAT-009
slug: auth-refresh-rotation
epic: 002-auth
milestone: 002-auth
sdd: SDD-001
frd: F5
status: planned
depends-on: [007-auth-signin]
blocks: [010-auth-signout]
date: 2026-06-07
---

# FEAT-009 — refresh rotation + reuse-detection ★

Realizes [[../02-epics/002-auth|EPIC-002 — auth]] · [[../01-milestones/002-auth|MILESTONE-002]].
Build of **[[../../architecture/sdds/sdd-auth|SDD-001]] §8 F5** — the **centerpiece**. Acceptance + tasks
(incl. invariants §4.1–4.4) are the SDD's.

## Intent

Exchange a refresh token for a new access + refresh, rotating **atomically** (`@Lock(PESSIMISTIC_WRITE)`);
detect reuse of a rotated/revoked token and **revoke the whole family** (the stolen-refresh defense).

## Sequence

- **Depends on:** F3 sign-in (the root session it rotates).
- **Blocks:** F6 sign-out (reuses the `revokeFamily` / revocation path built here).
- The epic's hardest feature — exercises the concurrency race (two rotations → one wins, the other is reuse).

## Done when

SDD-001 §8 F5 acceptance + invariants 1–4 met (rotate chains + slides; reuse → 401 + family revoked;
concurrent race; expired → 401) + feature DoD (integration tests: rotate, reuse-after-rotate, concurrent
race, revoked, expired; CI green; PR into `epic/002-auth`).
