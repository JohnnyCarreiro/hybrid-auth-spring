# Hybrid Auth Spring — Open Questions

Running list of things we haven't decided. Keep entries dated. Move to ADRs when resolved.

## Active

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
- [x] Resource-server verification = a **hand-built** singleton JWKS verifier wired into Spring's resource-server chain, **refining** the earlier "via `jwk-set-uri`" decision (for showcase symmetry with the hand-built issuer) — `adrs/0005-resource-server-jwks-verifier.md`, 2026-06-08 (FEAT-011).
- [x] Identity update propagation (OQ-007) = **event-driven** (auth emits `user.created`/`user.updated`; resource consumes), preferred over a callback; provisioning runs before authz (self-keyed identity cache). Direction decided; **implementation deferred** (create-only at MVP) — `adrs/0006-user-identity-sync.md` + `adrs/0007-identity-mirror-sync-events.md`, 2026-06-08.
- [x] OpenAPI/Swagger UI + observability + automated E2E (OQ-008) = **deferred** for the showcase; a real deployment would carry all three. Stand-in: hand-written API reference in the README — `adrs/0008-deferred-operational-surface.md`, 2026-06-08.
