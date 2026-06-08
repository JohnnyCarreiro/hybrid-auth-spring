---
id: FEAT-006
slug: auth-jwks
epic: 002-auth
milestone: 002-auth
sdd: SDD-001
frd: F2
status: done
depends-on: []
blocks: [007-auth-signin, 008-auth-me]
date: 2026-06-07
closed: 2026-06-07
---

# FEAT-006 — JWKS + signing keys

Realizes [[../02-epics/002-auth|EPIC-002 — auth]] · [[../01-milestones/002-auth|MILESTONE-002]].
Build of **[[../../architecture/sdds/sdd-auth|SDD-001]] §8 F2** — JWKS + signing keys (the issuance
foundation). Acceptance + tasks are the SDD's.

## Intent

Stand up the RS256 key set with mint/verify capability and publish the public keys via
`/.well-known/jwks.json`; lazy rotation (90 d + 30 d grace); private key encrypted at rest, never served.

## Sequence

- **Depends on:** — (parallelizable with F1; both are foundations).
- **Blocks:** F3 sign-in (needs a key to mint), F4 `/me` (needs keys to verify).
- **Why it moved up** (vs the SDD's old reading order): minting an access JWT in sign-in requires a
  `JWKSource` + key, so the key foundation must land before F3 — see SDD §8 ordering note.

## Done when

SDD-001 §8 F2 acceptance met (JWKS serves active + grace keys, `max-age=600`; rotation keeps grace-window
verification; private key encrypted, never served) + feature DoD (tests, CI green, PR into `epic/002-auth`).

## Retro (2026-06-07)

Shipped on `feat/006-auth-jwks` (commit `2b2572d`), merged into `epic/002-auth`.

**What landed.** `jwks/` package: `SigningKey` aggregate (domain-owned UUID v7 that **is** the JWK `kid`;
infra-free — holds the public JWK JSON, the sealed private blob, and the `expiresAt` lifecycle); `PrivateKeyCipher`
(AES-256-GCM, per-encryption random IV + 128-bit tag, key from `AUTH_JWKS_ENC_KEY`); `SigningKeys` service
(generation, active key, lazy + out-of-band rotation at 90 d + 30 d grace pinned in code, public-only set);
`GET /.well-known/jwks.json` with `Cache-Control: public, max-age=600`; a `NimbusJwtEncoder` over a DB-backed
`JWKSource` resolving the active key. `V3__jwks.sql`. Proven by a Testcontainers IT (serve/cache/no-private-params,
rotation + grace verification, encrypted-at-rest, past-grace-not-served) + `PrivateKeyCipher` unit cases.

**Decisions / deviations.** No HTTP rotate endpoint yet — unauthenticated until F4's filter chain; rotation
stays an in-process admin op (`SigningKeys.rotate()`), exercised by tests. The shared Testcontainers base
(`AbstractAuthIT`) moved to the **singleton-container** pattern: `@Container`/`@Testcontainers` stops the
container after the first IT class, which broke the second class's context — one JVM-wide container shared
across ITs fixes it.

**Hand-off to F3.** Mint via the `JwtEncoder` bean; set the JWS header `kid` from `SigningKeys.activeSigningKey()`
so every access token's `kid` resolves to a served JWKS key (invariant 8); claims `sub`/`email`/`jti` + `iat`/`exp`
(15-min access TTL, pinned).
