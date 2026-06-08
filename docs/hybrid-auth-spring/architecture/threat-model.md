# Hybrid Auth Spring — Threat Model

The project handles credentials, signing keys, and untrusted input, so this file is kept (per the
template's "keep if…" rule). It expands the summary in `srs+sad.md` §2.5. Scope is the two services in
this repo (`auth-service`, `resource-service`); the clients and BFFs from the system-context diagram
(`srs+sad.md` §2.1) are noted where they own a boundary.

## Trust boundaries

- **Untrusted:** browsers / native clients, and anything they send (request bodies, headers, tokens).
- **Trusted (assumed):** the BFFs (server-side, per client — they hold the browser session), the
  private network the services run on, and the secret/credential delivery channel (environment / secret
  manager). TLS is assumed to terminate **upstream** (gateway / ingress / BFF); the services speak HTTP
  behind it.
- **Source of truth:** PostgreSQL. Redis (when introduced) is a cache only and may fail open.
- The **cookie-bearing edge is BFF ↔ browser**. Every edge inward (BFF → service, service → service)
  carries a **bearer token, no ambient credential** — which is the crux of the CSRF analysis below.

## What this project defends against (in scope)

| # | Threat | Defense (where) |
|---|--------|------------------|
| T1 | Password database leak | Passwords stored only as an **Argon2id** hash (`infra/config/CryptoConfig`, Spring `defaultsForSpringSecurity_v5_8`); raw password never persisted or logged. |
| T2 | Stolen / replayed refresh token | **Rotation + reuse-detection**: each rotation issues a new refresh and stamps the old `rotated_at`; presenting a rotated/revoked token **revokes the whole family** (`services/RotateTokenService`, SDD-001 §4). |
| T3 | Refresh-token store leak | Refresh tokens persisted only as a **SHA-256 hash** (`domain/token/RefreshTokens`); a leaked hash is not a usable token (lookup is by hash, the opaque value is never stored). |
| T4 | Signing **private** key compromise | Private JWK **AES-256-GCM encrypted at rest** (`domain/signing/PrivateKeyCipher`), key from `AUTH_JWKS_ENC_KEY` (env, never committed); JWKS serves **public keys only**; the private half never leaves the process. |
| T5 | Forged / tampered access token | **RS256 signature verified locally** against the JWKS (`infra/security/SecurityConfig` `JwtDecoder`); expired/invalid tokens rejected by Nimbus default validators. The resource-service verifies the same way — **no shared secret**, so a verifier can never mint. |
| T6 | Token outlives a revocation | Short **15-min access TTL** bounds the staleness window; the refresh session is the revocable source of truth. TTLs pinned in code (ADR-0002). |
| T7 | User enumeration (response or timing oracle) | Unknown email and wrong password return the **same** `401 INVALID_CREDENTIALS`, and a **decoy Argon2 verify** runs on the unknown-email path so timing matches (`services/SignInService`). |
| T8 | Concurrent-refresh race (double rotation) | `SELECT … FOR UPDATE` pessimistic lock serializes rotations of the same token; the loser observes `rotated_at` set and is treated as reuse (`SessionRepository.findByTokenHashForUpdate`, SDD-001 §4 inv. 3). |
| T9 | SQL injection | Spring Data JPA / parameterized queries only; no string-built SQL. |
| T10 | **CSRF** | **Not applicable by construction** — stateless token APIs, no auth cookie. See [CSRF posture](#csrf-posture). |
| T11 | Secrets committed to the repo | `.env` git-ignored (`.env.example` only); RS256 dev keys never committed (`*.pem`/`*.key` ignored); the test-only JWKS cipher key is clearly labelled non-secret. |

## Out of scope (not defended against here)

| # | Non-goal | Where it belongs / mitigation |
|---|----------|-------------------------------|
| N1 | Brute-force / credential stuffing / account lockout | Rate limiting deferred (Phase 2); put a limiter at the gateway/BFF. Argon2id already slows online guessing. |
| N2 | Transport security (TLS) | Assumed terminated upstream (gateway/ingress/BFF); the services run HTTP behind it. |
| N3 | Browser-side token theft (XSS) | Client/BFF concern — the BFF keeps tokens out of the browser (HttpOnly cookies); the access JWT is short-lived to bound exposure. |
| N4 | **CSRF at the BFF ↔ browser edge** | The **BFF's** responsibility (SameSite cookies + anti-CSRF token). See [CSRF posture](#csrf-posture). |
| N5 | OAuth/social login, RBAC/roles, email verification | Deferred (Phase 2). |
| N6 | DoS / resource exhaustion | Out of scope; note Argon2id and RSA keygen are CPU-heavy — the 200-char password cap bounds Argon2 input, and key generation is lazy/amortized. |

## Trust assumptions

- TLS terminates upstream; the services sit on a trusted network behind a gateway/BFF.
- `AUTH_JWKS_ENC_KEY` and the datasource credentials arrive via the environment / a secret manager — not
  the repository.
- BFFs are trusted server-side components (they hold sessions on behalf of browsers). Browsers and
  native clients are untrusted.
- Postgres is the source of truth; a Redis outage degrades performance, not correctness (fail-open).

## Security-relevant defaults

- **Spring Security** (`infra/security/SecurityConfig`): `SessionCreationPolicy.STATELESS`; CSRF
  **disabled deliberately** (see below); every route authenticated except the explicit public set
  (`/auth/sign-up|sign-in|token|sign-out`, `/.well-known/jwks.json`, `/health`);
  `oauth2ResourceServer().jwt()` with a local JWKS `JwtDecoder` (RS256 only, default validators).
- **Pinned lifetimes** (ADR-0002, not env-overridable): access **15 min**, refresh **7 d** sliding,
  signing-key rotation **90 d + 30 d grace**.
- **Crypto:** Argon2id (m=19456, t=2, p=1, 16-byte salt, 32-byte hash) for passwords; **RS256 / RSA-2048**
  for access tokens; **SHA-256** for refresh-token-at-rest; **AES-256-GCM** for the private JWK at rest.
- **Persistence:** `ddl-auto: validate` — Flyway owns the schema; entities must match.
- **Git hygiene:** `.gitignore` blocks `.env`, `*.pem`, `*.key`.

## CSRF posture

**Short version: the services in this repo are CSRF-immune by construction, so Spring Security's CSRF
filter is disabled on purpose — not by oversight. CSRF defense is real and necessary, but it lives at
the BFF ↔ browser edge, which this repo does not implement.**

**Why CSRF doesn't apply here.** A CSRF attack works because a browser **automatically attaches an
ambient credential** — a session cookie — to a cross-site request, so an attacker page can trigger a
state-changing call that rides the victim's logged-in session. These services carry **no ambient
credential**:

- The **access JWT** is read from the `Authorization: Bearer` header. A cross-site `<form>`, `<img>`,
  or top-level navigation **cannot set a custom header**; only same-origin JavaScript can, and the
  attacker's origin is neither same-origin nor in possession of the token.
- The **refresh token** is sent in the **JSON request body** (`POST /auth/token`, `POST /auth/sign-out`).
  An attacker can neither read it (to include it) nor cause the browser to attach it automatically.
- There is **no server-side session cookie** (`SessionCreationPolicy.STATELESS`).

With nothing ambient to forge, a CSRF token would protect nothing — hence `http.csrf(csrf ->
csrf.disable())` in `SecurityConfig`. (Spring enables CSRF by default because its default assumption is
**cookie-based session auth**; that assumption does not hold here.)

**The caveat that would flip this.** If the auth-service ever served a **browser directly with a session
cookie** (no BFF in front), CSRF would immediately apply and must be re-enabled — or replaced with
`SameSite=Strict`/`Lax` cookies **plus** a custom-header / double-submit check. Any move to cookie-borne
auth is a CSRF-relevant change.

**Where CSRF actually lives in the full architecture (out of scope here).** In the system-context
diagram (`srs+sad.md` §2.1), the **BFF holds the browser session in an HttpOnly cookie** — that cookie
*is* an ambient credential, so the **BFF** must defend it:

- `SameSite=Lax` (or `Strict`) on the session cookie — the first-line modern mitigation.
- An anti-CSRF token (synchronizer or double-submit) on state-changing routes.
- `Origin` / `Referer` validation on mutations.

That is a BFF responsibility, not the auth-service's — but it is the other half of the story, which is
why the boundary split in the system-context diagram matters.

**CORS footnote.** Because the services hold no cookie, CORS here is API hygiene, not CSRF defense. They
are normally called **server-to-server** (by BFFs), so no browser CORS grant is needed; if a browser is
ever pointed straight at them, configure a strict CORS policy (explicit origins, never `*` with
credentials) — but that is about who may *script* the API, not about CSRF.
