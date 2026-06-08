# ADR-0005 — Resource-server token verification: a hand-built JWKS verifier

- **Status:** Accepted
- **Date:** 2026-06-08
- **Milestone / Sprint:** 3 (resource)
- **Refines:** ADR-0002 (which resolved "resource server via `jwk-set-uri`")

## Context

The resource-service must verify each Bearer access JWT **locally** against the auth-service's
published JWKS, with no shared secret and no per-request call to auth (SRS+SAD §2.1 / SDD-001 §4
invariant 6). ADR-0002 and `open-questions.md` resolved this as "resource server via `jwk-set-uri`" —
i.e. Spring Security's turnkey `NimbusJwtDecoder.withJwkSetUri(...)`, which already keeps an in-memory
cache of the JWKS and refetches when it meets an unknown `kid` (a rotation).

That built-in already satisfies the functional requirement. But two things pull the other way:

1. **This is a reference/showcase.** The auth-service deliberately **hand-builds** its RS256 issuer and
   its own `JwtDecoder` (ADR-0002) rather than using Spring Authorization Server, so the mechanics are
   visible and reviewable. A turnkey `jwk-set-uri` one-liner on the verify side hides exactly the
   mechanism the project exists to demonstrate — the asymmetry would be didactically lopsided.
2. **The desired behavior is explicit and worth owning:** a singleton holding the public keys in
   memory; on a verify miss, refetch the JWKS once (keys may have rotated) and retry; on a second
   failure, 401. Stating it as our own component makes the contract and its edge cases (forged token,
   issuer down) testable in isolation.

## Decision

Implement a **hand-built singleton verifier**, `JwksTokenVerifier`, as the Spring Security
`JwtDecoder`, wired into the standard `oauth2ResourceServer().jwt()` filter chain (so the filter,
`JwtAuthenticationToken`, and 401 rendering remain idiomatic Spring).

- **In-memory cache.** The verifier holds a `NimbusJwtDecoder` closed over the last-fetched public
  `JWKSet`. The happy path verifies against the cache with **no** network call.
- **Refetch-once, then 401.** On any verify failure — unknown `kid` (rotation) **or** bad signature —
  it refetches `/.well-known/jwks.json` exactly once, rebuilds the decoder, and retries. A second
  failure propagates as a `JwtException`, which the resource-server filter renders as **401**.
- **Same verification core as auth.** RS256 only, `DefaultJWTProcessor` + `JWSVerificationKeySelector`,
  Nimbus default validators (exp/nbf) — identical to the auth-service's `SecurityConfig#jwtDecoder`; the
  only difference is the key *source* (there: in-process public set; here: the remote JWKS, cached).
- **Configuration.** The JWKS URL is `resource.auth.jwks-uri` (env `RESOURCE_AUTH_JWKS_URI`), defaulting
  to the auth-service on `:3333`; in docker-compose it points at `http://auth-service:3333/...`.

The resource-service **does not** mint or rotate tokens — refresh rotation is the BFF↔auth concern.

## Alternatives considered

- **(a) Spring `jwk-set-uri` decoder (ADR-0002 as written)** — least code; its `RemoteJWKSet`/cache
  already does in-memory caching + refresh-on-unknown-kid, so it meets the requirement. Rejected *here*
  only because it hides the mechanism this reference exists to show; it remains the right call in a
  non-showcase service, and our hand-built version is behavior-compatible with it.
- **(c) Fully custom `OncePerRequestFilter`** — maximum explicitness but re-implements what the
  resource-server filter already does well (bearer extraction, `Authentication` population, 401
  rendering) and diverges hardest from idiomatic Spring. Rejected — the `JwtDecoder` seam gives all the
  explicitness we want with none of that loss.

## Consequences

- **Positive:** the verify mechanics are explicit, owned, and unit-tested in isolation (rotation,
  forged, expired) against an in-JVM JWKS stub; symmetric with the hand-built issuer; still rides the
  idiomatic resource-server filter chain; no shared secret; happy path makes no per-request call.
- **Negative / follow-up:** a *forged* token (known `kid`, bad signature) costs one extra (cacheable,
  cheap) JWKS fetch before the 401 — bounded to one refetch per request; a future refinement could skip
  the refetch when the `kid` is already cached. The cache has no TTL/background refresh — it only
  updates reactively on a miss; acceptable because the auth-service serves keys with a long rotation
  grace window (SDD-001 §8 F2). If the issuer is unreachable, verification fails closed (401), not 500.

## References

- `0002-auth-stack-handbuilt-rs256-issuer.md` (the decision this refines), `../sdds/sdd-tasks.md` §6 +
  §8 F1, `../sdds/sdd-auth.md` §7 (the JWKS endpoint consumed), `../srs+sad.md` §2.1/§2.5,
  `../../roadmap/03-features/011-resource-jwt-verifier.md`.
