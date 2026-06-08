# AGENTS.md — `resource-service`

The MVC task/project manager: the **resource-server** half of the hybrid scheme. Read this before
touching code here; the root `../AGENTS.md` and the playbook still apply on top.

## Responsibility

Owns the **task/project domain** (`projects`, `tasks`) and a **local identity mirror** (`app.users`)
of a subset of auth identity. Verifies the access JWT locally against the auth-service JWKS and
authorizes every operation by **ownership**. Consumes Postgres (the isolated `app` DB — ADR-0003).
**Does not own** identity, sessions, or signing keys, **never mints tokens, and never rotates the
refresh token** — that is the BFF's job against the auth-service. The only inbound dependency on auth
is the published JWKS document (one-way; SDD-002 §1).

## Boundaries

- **Depends on:** Postgres (`app` DB), the auth-service JWKS endpoint (`GET /.well-known/jwks.json`,
  HTTP, read-only), Spring Security 6 resource-server + Nimbus JOSE (local JWT verify).
- **Consumed by:** BFFs / clients presenting `Authorization: Bearer <access JWT>`.
- **Cross-context comms:** outbound only — fetch the JWKS (cached, refetched on rotation). No shared
  DB, no shared secret, no per-request call to auth on the happy path.

## Layout

Organized **by layer** (same pragmatic Clean-Arch/DDD-*like* shape as the auth-service), under
`src/main/java/.../resource/`:

```
resource-service/.../resource/
├── domain/         — the model (born-consistent aggregates) + domain errors
│   ├── identity/   AppUser (the create-only mirror of auth identity)
│   ├── project/    Project + ProjectNotFoundException
│   ├── task/       Task · TaskStatus + TaskNotFoundException
│   └── shared/     IdMint (UUID v7, for project/task only) · ResourceException · ResourceErrorCode
├── services/       — application use cases: ProjectService · TaskService · UserMirror · CurrentUser
├── web/            — controllers + request/response DTOs (by feature) + ResourceExceptionHandler
└── infra/          — database/ (3 JpaRepositories) · security/ (SecurityConfig, JwksTokenVerifier) ·
                       web/ (MirrorSyncInterceptor, WebConfig)
src/main/resources/db/migration/  — Flyway (app DB): V1 baseline · V2 users (mirror) · V3 projects · V4 tasks
```

`web → services → domain`, `infra → domain`. The verifier (`infra/security`) imports Nimbus, like the
auth-service's signing code — the boundary kept framework-free is the *aggregates*, not all of infra.

## Commands

```bash
# JDK 21 is via SDKMAN; gradle needs it on PATH:
export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.5-tem" && export PATH="$JAVA_HOME/bin:$PATH"

./gradlew :resource-service:test          # unit (verifier) + Testcontainers integration (ADR-0001)
./gradlew :resource-service:bootRun       # needs `docker compose up -d postgres` and the auth-service
                                          # reachable at RESOURCE_AUTH_JWKS_URI (default :3333)
./gradlew spotlessApply                   # google-java-format (CI gate: spotlessCheck)
```

> **Testcontainers on recent Docker Engines.** Same gotcha as the auth-service: export
> `DOCKER_HOST=unix:///var/run/docker.sock` and `DOCKER_API_VERSION=1.44`; the root build forwards
> them to docker-java. Without them the build auto-negotiates (older daemons / CI).

## Conventions specific to this module

- **Local verify, no shared secret (ADR-0005).** The Bearer JWT is verified in-process against the
  auth-service's *public* JWKS by `JwksTokenVerifier` — a singleton `JwtDecoder` that caches the keys
  in memory and refetches `/.well-known/jwks.json` **once** on a verify miss (key rotation), then 401s
  on a second failure. Never add a shared symmetric secret; never call auth per request.
- **Ownership is the authz boundary, and 404 ≠ 403.** Every project/task operation is scoped to the
  token `sub`. A resource that does not exist *or* is owned by someone else returns **404**
  (`ProjectNotFound`/`TaskNotFound`) — the API never confirms another user's data. Task ownership is
  **derived** through the parent project; a task carries no `owner_id`.
- **The user mirror is create-only (ADR-0006).** `app.users` is provisioned JIT on the first
  authenticated request via `INSERT … ON CONFLICT (id) DO NOTHING`. It is **never updated** from a
  later token — propagating auth-side changes is deferred to an event (see SDD-002 §8 F-sync). Do not
  "upsert" identity fields on every request.
- **Domain-owned identity, with one exception.** `Project`/`Task` mint their own UUID v7 via
  `domain/shared/IdMint` (never `@GeneratedValue`, never `UUID.randomUUID()`). **`AppUser.id` is the
  exception** — it is the auth `sub`, copied in, never minted here.
- **Aggregates are born consistent.** No public constructors; static factories (`Project.create`,
  `Task.create`) are the only way in. `ddl-auto: validate` — Flyway owns the schema; entities must match.
- **One web error edge.** Typed `ResourceException`s carry a `ResourceErrorCode`; the single
  `web/ResourceExceptionHandler` is the only place a code becomes an HTTP status (RFC 7807 ProblemDetail
  with a stable `code`). 401s come from Spring Security's filter, never from this handler.

## Points of attention

- ID generation lives in `domain/shared/IdMint` — never define a local UUID helper. It is **not** used
  for `AppUser` (auth owns that id).
- The error vocabulary is `domain/shared/ResourceErrorCode` — add a variant there (with its status)
  rather than throwing ad-hoc statuses from controllers.
- The mirror-sync seam is `infra/web/MirrorSyncInterceptor` (provision before the controller runs, so
  the `projects.owner_id` FK is satisfiable). It is excluded from `/health`.
- This service **must not** issue or rotate tokens. If a feature seems to need that, it belongs in the
  auth-service or the BFF, not here (SDD-002 §1).

## References

- [[docs/hybrid-auth-spring/architecture/sdds/sdd-tasks.md]] — tactical bible for this domain (SDD-002).
- [[docs/hybrid-auth-spring/architecture/adrs/0005-resource-server-jwks-verifier.md]] — the hand-built local JWT verifier.
- [[docs/hybrid-auth-spring/architecture/adrs/0006-user-identity-sync.md]] — create-only mirror provisioning; deferred event-based updates.
- [[docs/hybrid-auth-spring/architecture/adrs/0003-database-per-service-isolation.md]] — the isolated `app` DB this service owns.
- [[docs/hybrid-auth-spring/architecture/sdds/sdd-auth.md]] — the auth domain whose JWKS this service consumes (SDD-001).
```
