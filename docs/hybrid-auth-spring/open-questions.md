# Hybrid Auth Spring — Open Questions

Running list of things we haven't decided. Keep entries dated. Move to ADRs when resolved.

## Active

### OQ-005 — UUID v7 generator (2026-06-05)

`users`/`sessions`/`jwks` ids are UUID v7 (time-ordered — better index locality, mirrors the reference
design). The JDK has no built-in v7 generator.

Options:
- (a) A dedicated library (e.g. `uuid-creator`) — explicit, isolated from the ORM.
- (b) Hibernate 6.5 `@UuidGenerator` — no extra dependency, but couples id generation to the ORM.

Leaning (a). Decide while building F1 (sign-up) — see `architecture/sdds/sdd-auth.md` §8.

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
