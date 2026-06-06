# Hybrid Auth Spring — Open Questions

Running list of things we haven't decided. Keep entries dated. Move to ADRs when resolved.

## Active

### OQ-001 — No `playbook-java.md` in the template (2026-06-05)

The project-template ships only `playbook-base.md`, `playbook-rust.md`, and `playbook-ts.md`. There is
no Java playbook, so only `playbook-base.md` was copied into `architecture/playbook/`. The base
playbook is language-agnostic and fully applies; the Java-specific addendum (idioms, Spring patterns,
build conventions) is missing.

Options:
- (a) Author a generic `playbook-java.md` in the shared template repo (`templates/project_templates/playbooks/`) so every future Java project inherits it.
- (b) Author a project-local `playbook-java.md` here only, promote to the template later if it generalizes.
- (c) Stay on `playbook-base.md` alone for this showcase; defer a Java playbook entirely.

Leaning (b) — capture Java/Spring conventions as we build, promote to the template if they hold. Decide during/after Pass 1.

### OQ-002 — Java CI workflow (2026-06-05)

`_shared/github-workflows/` only provides `rust-ci.yml` and `lint-and-typecheck-ts.yml`. No Java/Gradle
CI was copied. `.github/workflows/` is empty.

Options:
- (a) Add a Gradle CI workflow (build + test + format check) and contribute it back to the template's `_shared/github-workflows/`.
- (b) Add a project-local Gradle CI workflow only.

Leaning (a). Decide when the build exists (Pass 1/Pass 4). No remote yet, so not blocking.

### OQ-003 — Lint/format toolchain for Java (2026-06-05)

AGENTS.md currently assumes **Spotless + google-java-format** (`./gradlew spotlessCheck`). Not yet wired.

Options:
- (a) Spotless + google-java-format (assumed default).
- (b) Checkstyle + a separate formatter.
- (c) Palantir Java Format via Spotless.

Leaning (a). Confirm in Pass 1 when the Gradle build is set up.

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
