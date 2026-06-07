---
id: FEAT-007
slug: auth-signin
epic: 002-auth
milestone: 002-auth
sdd: SDD-001
frd: F3
status: planned
depends-on: [005-auth-signup, 006-auth-jwks]
blocks: [008-auth-me, 009-auth-refresh-rotation, 010-auth-signout]
date: 2026-06-07
---

# FEAT-007 — sign-in → hybrid credentials

Realizes [[../02-epics/002-auth|EPIC-002 — auth]] · [[../01-milestones/002-auth|MILESTONE-002]].
Build of **[[../../architecture/sdds/sdd-auth|SDD-001]] §8 F3** — Sign-in. Acceptance + tasks are the SDD's.

## Intent

Verify credentials, open a **root session** (fresh `family_id`, `parent_id NULL`), and return
`{ accessToken (RS256 JWT), refreshToken }`. The JWT verifies against the JWKS and carries `sub/email/jti`.

## Sequence

- **Depends on:** F1 sign-up (`users`) + F2 JWKS/keys (a signing key to mint).
- **Blocks:** F4 `/me`, F5 rotation, F6 sign-out (all need a session / a JWT).

## Done when

SDD-001 §8 F3 acceptance met (good creds → 200 + both tokens + a root `sessions` row; bad → 401; JWT
verifies) + feature DoD (integration test for token shape + session shape, CI green, PR into `epic/002-auth`).
