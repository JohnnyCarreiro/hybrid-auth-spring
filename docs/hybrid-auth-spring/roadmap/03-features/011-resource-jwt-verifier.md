---
id: FEAT-011
slug: resource-jwt-verifier
epic: 003-resource
milestone: 003-resource
sdd: SDD-002
frd: F1
status: in progress
depends-on: [006-auth-jwks]
blocks: [012-resource-user-mirror, 013-resource-projects-crud, 014-resource-tasks-crud]
date: 2026-06-08
---

# FEAT-011 — resource JWT verifier (F1) ★

Realizes [[../02-epics/003-resource|EPIC-003 — resource]] · [[../01-milestones/003-resource|MILESTONE-003]].
Build of **[[../../architecture/sdds/sdd-tasks|SDD-002]] §8 F1** — Local JWT verifier (JWKS, no shared secret).
Acceptance + tasks are the SDD's. Decision: [[../../architecture/adrs/0005-resource-server-jwks-verifier|ADR-0005]].

## Intent

Verify the Bearer access JWT **locally** against the auth-service public JWKS — a hand-built singleton
`JwtDecoder` that caches the keys in memory and refetches `/.well-known/jwks.json` once on a verify miss
(key rotation), then 401s on a second failure. Symmetric with the auth-service's hand-built issuer; no
shared secret, no per-request call on the happy path.

## Sequence

- **Depends on:** F2 auth JWKS (the endpoint it consumes — SDD-001 §7).
- **Blocks:** the mirror and all CRUD (every route is protected by this).
- The centerpiece of the resource side; refines ADR-0002's "resource server via `jwk-set-uri`".

## Done when

SDD-002 §8 F1 acceptance met (served-key token verifies; rotation picked up on next request with one
refetch; forged/expired → 401; happy path does not refetch; no shared secret) + feature DoD (verifier
unit test against an in-JVM JWKS stub, CI green, merged via PR).

## Build (2026-06-08)

**What landed.** `infra/security/JwksTokenVerifier` — a singleton `JwtDecoder` holding a
`NimbusJwtDecoder` closed over the last-fetched public `JWKSet`; `decode` verifies against the cache and,
on any `JwtException`, refetches the JWKS once (`JWKSet.load`, bounded timeouts), rebuilds, and retries —
a second failure propagates as 401. RS256 + Nimbus default validators, same core as auth's
`SecurityConfig#jwtDecoder`. `SecurityConfig`: stateless resource server, CSRF off by construction, every
route authenticated but `/health`. Config `resource.auth.jwks-uri` (env `RESOURCE_AUTH_JWKS_URI`,
compose → `http://auth-service:3333/...`).

**Tests.** `JwksTokenVerifierTest` against a JDK `HttpServer` JWKS stub: valid verify; **cache** (3
verifies → 1 fetch); **rotation** (cached k1 misses → refetch k2 → verifies); **forged** signature →
`JwtException`; **expired** → `JwtException`. Green.

**Status.** Built and green; **pending human validation (§16.3)** before the `feat/011…` branch / epic PR.
