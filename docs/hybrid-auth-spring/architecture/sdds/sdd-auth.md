---
id: SDD-001
slug: auth
title: Authentication (identity, sessions, signing keys)
subdomain-type: generic
status: draft
date: 2026-06-05
---

# SDD-001 — Authentication (identity, sessions, signing keys)

The tactical-design bible of the **auth** domain: how the auth-service issues and rotates hybrid
credentials and publishes its JWKS. The stack decision behind it is `../adrs/0002-auth-stack-handbuilt-rs256-issuer.md`;
this document is the mechanics. It is a port of a hybrid session+JWT / JWKS design the author runs in
production on another stack (better-auth + JWKS), reimplemented in Spring Security 6.

## 1. Domain / context

The auth-service owns **identity** (users + credentials), **sessions** (refresh tokens with rotation
lifecycle), and **signing keys** (the RS256 key set). It lives in the `auth-service/` Gradle module
with its own Postgres tables and (optionally) Redis as a hot-path cache.

The resource-service depends on this domain through **one** seam only: the public JWKS endpoint. The
reference is one-way — the resource-service fetches `/.well-known/jwks.json` and verifies tokens
locally; it never shares a database, a secret, or a per-request call with auth. Nothing in auth points
back at the resource domain.

## 2. Aggregates / domain types

- **User** — `id` (UUID v7), `email` (unique), `passwordHash` (Argon2id), `emailVerified` (bool,
  default false), `name`, `createdAt`, `updatedAt`.
- **Session (refresh) — the rotation aggregate.** `id` (UUID v7), `userId`, `tokenHash`
  (SHA-256 of the opaque refresh token, unique), `familyId`, `parentId` (self-ref, NULL on the family
  root), `expiresAt`, `rotatedAt`, `revokedAt`, `ipAddress`, `userAgent`, `createdAt`, `updatedAt`. The
  **family** — all sessions sharing a `familyId` — is the consistency boundary for reuse-detection.
- **SigningKey (jwks)** — `id` (= JWK `kid`), `publicKey` (JWK JSON), `privateKey` (encrypted at
  rest), `createdAt`, `expiresAt` (NULL = active; past = beyond grace, prunable).

Schema DDL: see §2.1. `familyId` carries **no DB default** — the application assigns it (a fresh UUID
on the root session, inherited on rotation) so the rule survives a store swap.

### 2.1 Schema

```sql
CREATE TABLE users (
  id            uuid PRIMARY KEY,
  email         text NOT NULL UNIQUE,
  password_hash text NOT NULL,
  email_verified boolean NOT NULL DEFAULT false,
  name          text,
  created_at    timestamptz NOT NULL DEFAULT now(),
  updated_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE sessions (
  id          uuid PRIMARY KEY,
  user_id     uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash  text NOT NULL UNIQUE,                 -- SHA-256 of the opaque refresh token
  expires_at  timestamptz NOT NULL,
  family_id   uuid NOT NULL,                         -- groups all rotation descendants of one login
  parent_id   uuid REFERENCES sessions(id) ON DELETE SET NULL,
  rotated_at  timestamptz,
  revoked_at  timestamptz,
  ip_address  text,
  user_agent  text,
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX sessions_user_id_idx       ON sessions(user_id);
CREATE INDEX sessions_family_id_idx     ON sessions(family_id);
CREATE INDEX sessions_family_active_idx ON sessions(family_id, rotated_at);

CREATE TABLE jwks (
  id          uuid PRIMARY KEY,                      -- = JWK kid
  public_key  text NOT NULL,
  private_key text NOT NULL,                         -- encrypted at rest (AES-GCM)
  created_at  timestamptz NOT NULL DEFAULT now(),
  expires_at  timestamptz
);
```

## 3. Use cases / operations

| Operation | Input | Output | Errors |
|-----------|-------|--------|--------|
| `signUp` | email, password | User (no auto-login at MVP) | `EmailAlreadyTaken`, `WeakPassword` |
| `signIn` | email, password | `{ accessToken, refreshToken, user }` | `InvalidCredentials` |
| `rotateToken` | refreshToken | `{ accessToken, refreshToken }` | `InvalidRefresh`, `ReuseDetected`, `SessionRevoked`, `SessionExpired` |
| `signOut` | refreshToken | void | `InvalidRefresh` |
| `getMe` | access JWT (Bearer) | User | `Unauthenticated` |
| `getJwks` | — | JWKSet (public) | — |
| `mintAccessToken` *(internal)* | userId | RS256 JWT | — |
| `rotateSigningKey` *(internal/admin)* | — | new SigningKey | — |

## 4. Invariants

Each gets at least one explicit test (ADR-0001).

1. **Family assignment.** A root session (from sign-up/sign-in) gets a fresh `family_id` and
   `parent_id = NULL`; a rotated session inherits the parent's `family_id` and sets `parent_id`.
   `family_id` is never null.
2. **Reuse ⇒ family death.** Presenting a refresh whose `rotated_at` **or** `revoked_at` is set ⇒
   revoke every still-active session in the family and reject (401). This is the stolen-refresh defense.
3. **Atomic rotation.** Rotation runs under `SELECT … FOR UPDATE` on the presented session. Two
   concurrent rotations of the same token produce **exactly one** new session; the loser observes
   `rotated_at` already set inside the transaction and is treated as reuse (family revoked).
4. **Sliding window.** `child.expires_at = now() + (parent.expires_at − parent.created_at)` — the
   original window length is preserved without storing a remember-me flag.
5. **Pinned lifetimes.** Access TTL = 15 min, refresh = 7 days; pinned in code, not env-overridable.
6. **Asymmetric boundary.** The private key never leaves auth-service; only the public JWKS crosses to
   the resource-service. No shared secret.
7. **Credential hygiene.** Passwords are persisted only as an Argon2id hash; refresh tokens only as a
   SHA-256 hash. Neither is ever stored or logged in clear. RS256 dev keys are never committed.
8. **Token identity.** Every access JWT carries a unique `jti`; its `kid` header resolves to a key
   currently served by `/.well-known/jwks.json`.

## 5. Errors

The auth error set and its HTTP mapping (the behavioral contract for the API surface):

| Variant | HTTP | When |
|---------|------|------|
| `EmailAlreadyTaken` | 409 | sign-up with an existing email |
| `WeakPassword` | 422 | password fails policy |
| `InvalidCredentials` | 401 | sign-in with wrong email/password |
| `InvalidRefresh` | 401 | refresh token unknown / malformed |
| `ReuseDetected` | 401 | rotated/revoked refresh presented → family revoked (logged as a security event) |
| `SessionRevoked` | 401 | session explicitly revoked (sign-out / family revocation) |
| `SessionExpired` | 401 | `expires_at` ≤ now |
| `Unauthenticated` | 401 | `/me` (or any protected route) without a valid access JWT |

`ReuseDetected`, `SessionRevoked`, `SessionExpired`, `InvalidRefresh` share HTTP 401 but stay distinct
internally (distinct codes + logs) so the rotation logic and tests can tell them apart.

## 6. Ports / external dependencies

- **Postgres** — the **`auth`** database (source of truth: `users`, `sessions`, `jwks`), isolated from
  the app store (ADR-0003), via Spring Data JPA. The rotation finder uses `@Lock(PESSIMISTIC_WRITE)`
  inside a `@Transactional` service → `SELECT … FOR UPDATE`.
- **Redis** — optional at MVP: refresh hot-path cache. Postgres remains the source of truth; a Redis
  outage degrades performance, not correctness (fail-open).
- **Nimbus JOSE** (`spring-security-oauth2-jose`) — RS256 signing (`NimbusJwtEncoder`) and `JWKSet`
  serialization.
- **Spring Security 6** — auth-service: a filter chain protecting `/me`; resource-service: configured
  as a resource server (`jwk-set-uri`).
- **springdoc-openapi** — Swagger UI on both services (dev profile).

## 7. Behavioral API surface

Routes name domain intentions, not status patches (playbook §6.1). `/auth/token` rotates; `/auth/sign-out`
revokes.

| Method | Route | Use case | Aggregate |
|--------|-------|----------|-----------|
| POST | `/auth/sign-up` | `signUp` | User |
| POST | `/auth/sign-in` | `signIn` | User + Session |
| POST | `/auth/token` | `rotateToken` | Session |
| POST | `/auth/sign-out` | `signOut` | Session |
| GET | `/me` | `getMe` | User |
| GET | `/.well-known/jwks.json` | `getJwks` | SigningKey |
| GET | `/health` | liveness | — |

## 8. Functionalities (child FRDs — absorbed at small tier)

One block per functionality: intent · acceptance · validation · Tasks (1–2 day units). At medium+ each
splits into its own `../frds/frd-<slug>.md`.

**Ordered by build/dependency sequence** (tactical — not the catalog order of §3/§7): the signing keys
precede sign-in (minting an access JWT needs a key), and a root session precedes rotation/sign-out. Each
block states its **Depends on** so the edge is explicit and the roadmap cards inherit the order 1:1.

### F1 — Email + password sign-up
- **Intent:** register a user with email + password; store the password only as an Argon2id hash.
- **Acceptance:** valid input → 200 + user (no token at MVP); duplicate email → 409; weak password → 422;
  the stored row has no clear-text password.
- **Validation:** email normalized lowercase + format; password policy (min length, etc.).
- **Tasks:** User entity + migration · `Argon2PasswordEncoder` bean · `signUp` use-case + route ·
  unit (policy) + integration (persisted hash, duplicate) tests.

### F2 — JWKS + signing keys
- **Intent:** stand up the RS256 key set with mint/verify capability and publish the public keys; rotate
  on cadence; verify tokens during the grace window. *(Issuance foundation — must exist before sign-in can
  mint an access JWT.)*
- **Acceptance:** `/.well-known/jwks.json` serves active (+ grace) keys with `Cache-Control: max-age=600`;
  a rotation adds a key and tokens signed by the previous key still verify within grace; private key is
  encrypted at rest and never served.
- **Validation:** RS256/2048; lazy rotation 90 d + 30 d grace; admin path for out-of-band rotation.
- **Tasks:** `jwks` table + encrypted key store · `JWKSource` + `NimbusJwtEncoder` · JWKS route ·
  rotation logic + admin trigger · integration test (serve, rotate, grace verify).

### F3 — Sign-in → hybrid credentials
- **Depends on:** F1 (users), F2 (a signing key to mint the access JWT).
- **Intent:** verify credentials, open a root session, return `{ accessToken (RS256 JWT), refreshToken }`.
- **Acceptance:** good creds → 200 with both tokens, a `sessions` row with fresh `family_id`,
  `parent_id NULL`; bad creds → 401; the JWT verifies against the JWKS and carries `sub/email/jti`.
- **Validation:** constant-time hash comparison (encoder handles it); no user-enumeration in error text.
- **Tasks:** `mintAccessToken` (Nimbus) · session creation + family root · `signIn` use-case + route ·
  integration test (token verifies, session shape).

### F4 — Current user (`/me`)
- **Depends on:** F3 (a JWT to present), F2 (keys to verify it).
- **Intent:** return the authenticated user for a valid access JWT. *(Closes the issue→verify loop with the
  smallest protected route before the rotation deep-dive.)*
- **Acceptance:** valid Bearer → 200 user payload; missing/invalid/expired → 401.
- **Tasks:** security filter chain (auth-service) · `getMe` route · integration test (valid + 401 paths).

### F5 — Refresh rotation + reuse-detection  *(centerpiece)*
- **Depends on:** F3 (the root session it rotates).
- **Intent:** exchange a refresh token for a new access + refresh, rotating atomically; detect reuse and
  revoke the family.
- **Acceptance:** valid refresh → 200 new pair, old session `rotated_at` set, new session chained
  (`parent_id`, same `family_id`, sliding `expires_at`); **reusing a rotated/revoked token → 401 and the
  whole family is revoked**; two concurrent rotations → one succeeds, the other is reuse; expired → 401.
- **Validation:** lookup by `token_hash`; `@Lock(PESSIMISTIC_WRITE)`; family revoke =
  `UPDATE sessions SET revoked_at=now() WHERE family_id=? AND revoked_at IS NULL`.
- **Tasks:** locked finder · `rotateToken` `@Transactional` use-case · `revokeFamily` · route + response
  shape · integration tests (rotate, reuse-after-rotate, concurrent race, revoked, expired).

### F6 — Sign-out (session revocation)
- **Depends on:** F3 (a session to revoke); reuses F5's `revokeFamily` / revocation path.
- **Intent:** revoke the presented session.
- **Acceptance:** `sign-out` → that session's `revoked_at` set; a subsequent rotate on it → 401; the
  access token lives out its ≤15 min TTL (accepted gap).
- **Tasks:** `signOut` use-case + route · integration test.

## 9. Open items

- `../../open-questions.md` **OQ-003** — Java lint/format toolchain (Argon2id pulls in BouncyCastle).
- **OQ-004** — token TTLs / key-rotation cadence: **pinned here** (15 min / 7 d sliding / 90 d + 30 d) per
  ADR-0002; entry moves to Resolved.
- **OQ-005** — UUID v7 generator (no JDK built-in) — library vs Hibernate generator. Decide in F1.
- Redis is optional at MVP (cache only); promote to required only if a hot-path measurement justifies it.
