# ADR-0002 — Auth-service token & credential stack: hand-built RS256 issuer on Spring Security 6

- **Status:** Accepted
- **Date:** 2026-06-05
- **Milestone / Sprint:** 0 (bootstrap)

## Context

The auth-service must issue hybrid credentials (server-side refresh session + short-lived RS256 JWT),
publish its public key via JWKS, and run refresh rotation with reuse-detection — the mechanics that
`srs+sad.md` §1.2/§2.4 names as the system's whole reason to exist. The resource-service must verify
those JWTs locally against the JWKS, with **no shared secret** (§1.3 NFR).

This design is a port: it mirrors a hybrid session+JWT / JWKS model the author already runs in
production on another stack (better-auth + JWKS, TypeScript), reimplemented idiomatically in Spring.
There the auth library supplied sign-up, sign-in, password hashing, session creation, JWT minting and
the JWKS plugin out of the box; the author hand-wrote only the rotation/reuse hooks. The Spring
ecosystem has **no equivalent batteries-included library**, so the decision is which parts to adopt
and which to build — and, critically, whether to hide or to demonstrate the rotation/reuse mechanics.

## Decision

We will **hand-build the token issuer on Spring Security 6**, not adopt an OAuth2/OIDC product. Concretely:

- **JWT signing (issuer):** Nimbus JOSE, via `spring-security-oauth2-jose` — `NimbusJwtEncoder` over a
  `JWKSource`, alg **RS256**, 2048-bit keys. Claims: `sub`, `email`, `email_verified`, `iat`, `exp`,
  unique `jti`; the `kid` header matches a published JWKS key.
- **JWKS:** an own endpoint `GET /.well-known/jwks.json` serving the **public** keys (active + grace
  window), `Cache-Control: public, max-age=600`. Keys live in a `jwks` table; the private key is
  **encrypted at rest** (AES-GCM, key from env). Rotation cadence 90 d + 30 d grace, lazy (a new key is
  minted on first issuance after the active one expires); an admin path covers out-of-band rotation.
- **JWT verification (resource-service):** `spring-boot-starter-oauth2-resource-server` with
  `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` pointing at the auth-service JWKS. Spring
  Security fetches, caches and verifies signatures locally — no shared secret, no per-request call to auth.
- **Passwords:** stored only as an **Argon2id** hash (`Argon2PasswordEncoder`, BouncyCastle).
- **Refresh tokens:** opaque 32-byte random (base64url), stored only as a **SHA-256 hash** (unique
  index, lookup by hash). Rotation + reuse-detection + family revocation per `sdd-auth` §4. This is
  tighter than the reference, which kept the session token in clear.
- **Token lifetimes pinned in code, not env-overridable:** access **15 min**, refresh **7 days**
  (sliding). A per-environment override would weaken the revocation SLA and stop exercising the
  refresh-on-401 path; pinning is intentional (resolves OQ-004).

## Alternatives considered

- **(a) Spring Authorization Server** — rejected: it models OAuth2.1 / OIDC grants (authorization-code,
  client-credentials) and abstracts token issuance behind that framework. The showcase's value is
  demonstrating the *hybrid session+JWT with refresh-family reuse-detection* explicitly; SAS has no
  first-class notion of refresh-token-family reuse-detection and would **hide the exact mechanics the
  repo exists to show**, while adding a heavier surface than two endpoints need.
- **(b) Keycloak / external IdP** — rejected: turns the project into "configure a product" instead of
  "show the engineering", hides the internals behind an opaque server, and adds operational weight that
  defeats the "small, copyable Spring reference" goal.
- **(c) Reuse the existing better-auth service as a Node sidecar** — rejected: the entire point is a
  Spring-idiomatic reimplementation; a Node sidecar is not a Java showcase.
- **(d) BCrypt + symmetric HS256** — rejected on the core thesis: HS256 requires sharing the signing
  secret with every verifier, contradicting "resource-service holds only the public key, no shared
  secret". (BCrypt itself would be an acceptable zero-dependency password fallback if Argon2id's
  BouncyCastle dependency ever becomes a problem; Argon2id is preferred to match the reference.)

## Consequences

- **Positive:** the repository shows the actual mechanics (signing, JWKS publication/rotation, refresh
  rotation/reuse-detection) rather than delegating them to a product — which is the point of a
  reference. The boundary thesis holds (asymmetric keys, no shared secret); resource-service validation
  is near-free via the resource-server starter; the stack stays idiomatic Spring.
- **Negative / follow-up:** more code than adopting a product, and key management/rotation is
  security-sensitive code we own. Mitigated by porting a proven design and covering the auth-critical
  paths with Testcontainers integration tests (ADR-0001). New `open-questions.md` OQ-005 tracks the
  UUID v7 generator choice (no JDK built-in). Argon2id pulls in BouncyCastle; the lint/format
  toolchain (OQ-003) stays open.

## References

- `../srs+sad.md` §1.2 (functional reqs), §1.3 (NFR — asymmetric signing, no shared secret), §2.4
  (dataflow), §2.5 (threat model).
- `../sdds/sdd-auth.md` — the tactical design this stack realizes (schema, rotation algorithm, JWKS).
- `0001-testing-stack-junit-mockito.md` — the test safety net for this security-critical code.
- `../../open-questions.md` — OQ-003 (lint/format), OQ-004 (TTLs — pinned here), OQ-005 (UUID v7).
