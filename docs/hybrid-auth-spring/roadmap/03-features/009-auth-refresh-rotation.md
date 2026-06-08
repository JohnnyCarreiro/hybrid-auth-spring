---
id: FEAT-009
slug: auth-refresh-rotation
epic: 002-auth
milestone: 002-auth
sdd: SDD-001
frd: F5
status: done
depends-on: [007-auth-signin]
blocks: [010-auth-signout]
date: 2026-06-07
closed: 2026-06-08
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

## Retro (2026-06-08)

Shipped on `feat/009-auth-refresh-rotation` (commit `029550a`), merged into `epic/002-auth`.

**What landed.** `RotateTokenService` — the one-transaction algorithm: locked lookup (`@Lock(PESSIMISTIC_WRITE)`
→ `SELECT … FOR UPDATE`) → invalid / reuse / revoked / expired guards → mint new refresh, chain a child
(`Session.rotateChild`: inherit `familyId`, set `parentId`, sliding `expiresAt`), stamp parent `rotatedAt`,
mint a new access JWT. `SessionRepository` gained the locked finder + a one-shot `revokeFamily` bulk update.
Four 401 codes (`INVALID_REFRESH`, `REFRESH_REUSE_DETECTED`, `SESSION_REVOKED`, `SESSION_EXPIRED`).
`POST /auth/token`. Reuse is logged as a security event. Full suite 35 tests, incl. the concurrent
FOR-UPDATE race (run 3×, not flaky).

**Key design call — revoke must survive the 401.** The family revoke can't run in a `REQUIRES_NEW`
transaction: this method holds the `PESSIMISTIC_WRITE` lock on a row the revoke `UPDATE` targets, so a nested
tx self-deadlocks (confirmed live via pg lock inspection). Instead the revoke runs **inline** and the method
is `@Transactional(noRollbackFor = ReuseDetectedException.class)` — the revoke commits while the exception
still surfaces as the 401. (Corrects the originally-planned REQUIRES_NEW approach.)

**Hand-off to F6.** Add `Session.revoke(Instant)` (sibling of `rotate`); sign-out revokes the presented
session only (or its family) — the `SESSION_REVOKED` 401 on a later rotate is already wired and tested.
