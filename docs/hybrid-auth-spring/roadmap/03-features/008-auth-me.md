---
id: FEAT-008
slug: auth-me
epic: 002-auth
milestone: 002-auth
sdd: SDD-001
frd: F4
status: done
depends-on: [007-auth-signin, 006-auth-jwks]
blocks: []
date: 2026-06-07
closed: 2026-06-07
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

## Retro (2026-06-07)

Shipped on `feat/008-auth-me` (commit `6dfefac`), merged into `epic/002-auth`.

**What landed.** The auth-service's first Spring Security filter chain (`support/security/SecurityConfig`):
stateless, CSRF off, exact-matcher permitAll for the public surface (`/auth/{sign-up,sign-in,token,sign-out}`,
JWKS, `/health`), everything else authenticated, `oauth2ResourceServer().jwt()`. A local `JwtDecoder` verifies
access tokens **in-process** against `SigningKeys.publicJwkSet()` (active + grace, RS256, default validators) —
the auth-service is its own resource server, no self-HTTP, no shared secret. `GET /me` → `CurrentUser` use case
resolves the token `sub` to the live `User`; unknown subject → 401 `UNAUTHENTICATED`.

**Cross-cutting risk handled.** Adding `spring-boot-starter-oauth2-resource-server` auto-locks every route; the
permitAll matchers kept F1/F2/F3 green. Full suite 29 tests. The decoder deliberately does not reuse the issuer
`JWKSource` (private + active-only) so grace-window tokens still verify.

**Hand-off.** `/auth/token` (F5) and `/auth/sign-out` (F6) are already public (they authenticate by the opaque
refresh token in the body, not a Bearer JWT) — those features need no security change.
