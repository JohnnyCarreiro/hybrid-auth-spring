# AGENTS.md — `auth-service`

The auth microservice: the **issuer** half of the hybrid scheme. Read this before touching code here;
the root `../AGENTS.md` and the playbook still apply on top.

## Responsibility

Owns **identity** (users + credentials), **sessions** (refresh tokens + rotation lifecycle), and the
**signing keys** (RS256 key set + JWKS). Issues hybrid credentials and publishes its public keys.
Consumes Postgres (the isolated `auth` DB — ADR-0003) and, optionally, Redis (hot-path cache).
**Does not own** the projects/tasks domain and never calls the resource-service — the only seam out is
the public JWKS endpoint (one-way; SDD-001 §1).

## Boundaries

- **Depends on:** Postgres (`auth` DB), Nimbus JOSE (issuer signing), Spring Security 6 (filter chain),
  BouncyCastle (Argon2id).
- **Consumed by:** the resource-service — **only** via `GET /.well-known/jwks.json`. No shared DB, no
  shared secret, no per-request call.
- **Cross-context comms:** none inbound; outbound is the published JWKS only.

## Layout

```
auth-service/
├── identity/        — the User aggregate, Email value object, password policy, sign-up use case + web
├── support/         — cross-cutting: IdMint (UUID v7), CryptoConfig (Argon2id), error/ (typed + handler)
├── (sessions/)      — refresh sessions + rotation/reuse-detection            [arrives with F3/F5]
├── (jwks/)          — RS256 key set, encrypted private keys, JWKS endpoint   [arrives with F2]
└── src/main/resources/db/migration/  — Flyway: V1 baseline, V2 users, …
```

## Commands

```bash
# JDK 21 is via SDKMAN; the gradle invocations need it on PATH:
export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.5-tem" && export PATH="$JAVA_HOME/bin:$PATH"

./gradlew :auth-service:test          # unit + Testcontainers integration (ADR-0001)
./gradlew :auth-service:bootRun       # run locally (needs `docker compose up -d postgres redis`)
./gradlew spotlessApply               # google-java-format (CI gate: spotlessCheck)
```

> **Testcontainers on recent Docker Engines.** docker-java pings with an old API version (1.32) that
> Docker Engine ≥ a recent release rejects ("minimum supported API version is 1.40"). Export
> `DOCKER_HOST=unix:///var/run/docker.sock` and `DOCKER_API_VERSION=1.44`; the root build forwards
> `DOCKER_HOST` and maps the version to docker-java's `api.version` system property for the test JVM
> (see root `build.gradle.kts`). Without those env vars the build auto-negotiates (older daemons / CI).

## Conventions specific to this module

- **Domain-owned identity.** Aggregates assign their own UUID v7 via `support/IdMint` at construction;
  id generation is **never** delegated to Hibernate (`@GeneratedValue`). An aggregate is fully valid in
  memory before it touches a store. New ids come from `IdMint.next()` — never `UUID.randomUUID()` (v4).
- **Aggregates are born consistent.** No public constructors on entities — a static factory
  (`User.register(...)`) is the only way in. JPA gets a `protected` no-arg constructor, nothing more.
- **Domain stays infra-free.** Typed `AuthException`s carry an `AuthErrorCode` (a plain enum, no Spring
  types); the single `support/error/AuthExceptionHandler` is the only place a code becomes an HTTP
  status (RFC 7807 ProblemDetail with a stable `code` property). Errors carry the offending value /
  expected shape; nothing is swallowed (playbook §10).
- **Credential hygiene.** Only the Argon2id hash is persisted; the raw password never enters an
  aggregate and is never logged (SDD-001 §4, invariant 7). `ddl-auto: validate` — Flyway owns the
  schema; entities must match the migration.

## Points of attention

- ID generation lives in `support/IdMint` — never define a local UUID helper.
- The error vocabulary is `support/error/AuthErrorCode`; add a variant there (with its HTTP status)
  rather than throwing ad-hoc statuses from controllers.
- TTLs and key-rotation cadences are **pinned in code, not env-overridable** (ADR-0002 / SDD-001 §4
  invariant 5) — do not add properties to weaken them.
- Private signing keys are encrypted at rest and never leave the service (SDD-001 §4 invariant 6).

## References

- [[docs/hybrid-auth-spring/architecture/sdds/sdd-auth.md]] — tactical bible for this domain (SDD-001).
- [[docs/hybrid-auth-spring/architecture/adrs/0002-auth-stack-handbuilt-rs256-issuer.md]] — the issuer stack decision.
- [[docs/hybrid-auth-spring/architecture/adrs/0001-testing-stack-junit-mockito.md]] — JUnit 5 + Mockito + Testcontainers.
