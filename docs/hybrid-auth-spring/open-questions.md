# Hybrid Auth Spring — Open Questions

Running list of things we haven't decided. Keep entries dated. Move to ADRs when resolved.

## Active

### OQ-007 — identity update propagation: event vs callback (2026-06-08)

The `app.users` mirror is **create-only** at this tier (ADR-0006): provisioned JIT on first sight,
never updated from a later token. Propagating auth-side identity changes (email/name) is deferred. The
**recommended** path is an auth-emitted `user.updated` event the resource-service consumes; an outbound
callback from auth is the **less-favoured** alternative (couples the issuer to its consumers). Decide
the transport (and whether an outbox is warranted) when a second consumer or a real freshness
requirement appears. Tracked against SDD-002 §8 F-sync.

### OQ-008 — OpenAPI/Swagger UI not yet wired (2026-06-08)

REQ-012 (both services publish Swagger UI in the dev profile) is still `planned` — neither service has a
springdoc dependency. Add `springdoc-openapi` to both when the dev route-inspection surface is
prioritized; out of scope for the auth and resource MVP epics.

### OQ-006 — release-please auth: PAT → GitHub App (2026-06-07)

The release PR must run `main`'s required checks, so it is opened under a **fine-grained PAT**
(`secret RELEASE_PLEASE_TOKEN`) instead of the default `GITHUB_TOKEN` (whose PRs don't trigger workflows).
PAT works but expires (≤1 y) and acts as the human author. **Migrate to a GitHub App token**
(`actions/create-github-app-token`) — no expiry, bot identity, repo-scoped. Do it on a CI-config PR;
swap `token:` from the PAT secret to the app-token step output. Not urgent — the PAT unblocks `v0.1.0`.

## Resolved

- [x] Tier = `small` (combined `srs+sad.md`) — 2026-06-05.
- [x] Feature placement Mode = B (epic-bound, solo/kanban-flow) — 2026-06-05.
- [x] Stack = Java 21 · Spring Boot 3.5 · Spring Security 6 · Gradle · Postgres · Redis — 2026-06-05.
- [x] Second service domain = task/project manager (MVC, ownership-based authz) — 2026-06-05.
- [x] Token TTLs / key-rotation cadence (OQ-004) = access 15 min · refresh 7 d sliding · rotation 90 d + 30 d grace, pinned in code (not env-overridable) — `adrs/0002-auth-stack-handbuilt-rs256-issuer.md`, 2026-06-05.
- [x] Auth issuer stack = hand-built RS256 issuer on Spring Security 6 + Nimbus; resource server via `jwk-set-uri`; Argon2id passwords; refresh hashed at rest — `adrs/0002-auth-stack-handbuilt-rs256-issuer.md`, 2026-06-05.
- [x] Java playbook (OQ-001) authored — `architecture/playbook/playbook-java.md`, 2026-06-07 (FEAT-002).
- [x] CI (OQ-002) = GitHub Actions Gradle build+test (Testcontainers) + `spotlessCheck`; commit convention via commitlint on the PR title — 2026-06-07 (FEAT-002).
- [x] Lint/format (OQ-003) = Spotless + google-java-format — 2026-06-07 (FEAT-002).
- [x] UUID v7 generator (OQ-005) = option (a), the `uuid-creator` library, generated in the domain (the aggregate stamps its own id via `support/IdMint` — never the ORM) — 2026-06-07 (FEAT-005).
