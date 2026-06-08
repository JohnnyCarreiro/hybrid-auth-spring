---
id: FEAT-007
slug: auth-signin
epic: 002-auth
milestone: 002-auth
sdd: SDD-001
frd: F3
status: done
depends-on: [005-auth-signup, 006-auth-jwks]
blocks: [008-auth-me, 009-auth-refresh-rotation, 010-auth-signout]
date: 2026-06-07
closed: 2026-06-07
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

## Retro (2026-06-07)

Shipped on `feat/007-auth-signin` (commit `8cb9c0e`), merged into `epic/002-auth`.

**What landed.** `sessions/` package: `Session` aggregate (domain-owned UUID v7, `openRoot` → fresh
`familyId`/`parentId NULL`, full schema mapped) + `SessionRepository`; `RefreshTokens` (32-byte base64url
opaque token, SHA-256-hex at rest); `AccessTokens` (RS256 mint via F2's `JwtEncoder`, `sub/email/email_verified/jti`
claims + active `kid` header, 15-min TTL pinned); `SignInService` (verify → root session → hybrid pair,
7-day refresh TTL pinned). `POST /auth/sign-in`. `V4__sessions.sql` (full table + 3 indexes).

**Security touch.** No user enumeration in result **or timing**: an Argon2 verify runs on both branches —
against a decoy hash when the email is unknown — so unknown-email and wrong-password are indistinguishable.
(Added on review beyond the agent's first cut; SDD §8 only mandated no enumeration in error text.)

**Deferred to F5/F6** (fields present, mutators absent by design): `Session.rotate`/`revoke`, a rotation-child
factory (inherit `familyId`, set `parentId`, sliding `expires_at`), and the `@Lock(PESSIMISTIC_WRITE)`
`findByTokenHash` finder + family-revoke query. The `sessions_family_active_idx` already backs the family scan.
