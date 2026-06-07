# playbook-java.md — Java / Spring stack conventions

Stack-specific addendum to `playbook-base.md` for this project (resolves OQ-001). If anything here
conflicts with `playbook-base.md`, the base wins — flag it.

## 1. Toolchain & build

- **Java 21** (Gradle toolchain pins it — no reliance on the host JDK version).
- **Gradle, Kotlin DSL**, multi-module. Versions via the **version catalog** (`gradle/libs.versions.toml`).
- Modules: `auth-service`, `resource-service` (Spring Boot apps), `shared` (plain `java-library`,
  dependency-free until it prevents real duplication). Common config in the root `subprojects {}`.
- Build/run without a host JDK via Docker recipes (`docker-build`, `docker-up`); fast inner loop via
  host `bootRun` (`dev-run`/`dev-auth`/`dev-resource`).

## 2. Code style

- **google-java-format** via **Spotless** (`spotlessApply` / `spotlessCheck`, wired into `check`).
  `just fmt` formats; CI runs `spotlessCheck`. No hand-formatting debates.
- **No Lombok** — plain Java (records, constructors). Keep the reference copy-pasteable and tool-free.
- Package root: `com.johnnycarreiro.hybridauth`; per service `…​.auth` / `…​.resource`; shared `…​.shared`.

## 3. Web & runtime

- Embedded server: **Jetty** (ADR-0004), not the default Tomcat.
- `GET /health` via actuator remapped to the root base-path.
- Config via `application.yml`; all environment-specific values from env vars with sane local defaults
  (ports, datasource URLs). Never hard-code secrets.

## 4. Persistence

- **Spring Data JPA** + **PostgreSQL**; one **isolated database per service** (ADR-0003), accessed only
  by its owner with its own credentials.
- **Flyway** for migrations — plain versioned SQL under `src/main/resources/db/migration` (`V<n>__*.sql`),
  one history per service DB. `ddl-auto: validate` (Flyway owns the schema; Hibernate never mutates it).

## 5. Testing

- **JUnit 5** + **Mockito** (unit) and **Testcontainers** for anything touching Postgres/Redis
  (ADR-0001) — real engines, no H2. DB-backed `@SpringBootTest` uses `@ServiceConnection` on a
  `PostgreSQLContainer`.
- Testcontainers needs a native Docker env → tests run on the **host** (`./gradlew test`) or **CI**, not
  inside the dockerized-gradle build (`docker-build` = `assemble`, no tests).
- DoD: happy path + at least one negative case per feature.

## 6. Git flow, commits, CI

- Tier `small`: `feat/<NNN>` → `epic/<NNN>` (local) → **PR to `dev`**; `dev → main` = release (tag).
  `dev`/`main` protected (PR-only; `main` no-bypass) — see playbook-base §16.2.
- **Conventional Commits**: local `commit-msg` regex in a plain POSIX hook (`.githooks/`, wired via
  `core.hooksPath`; `just hooks-install` — no deps) + CI validates the **PR title**. commitlint lives
  **only in CI** (`commitlint.config.js`). (lefthook was evaluated but its hooks need a global/npm/PATH
  binary and auto-sync clobbers custom wrappers — plain hooks satisfy no-global/no-npm/local cleanly.)
- CI: `./gradlew build` (tests via runner Docker) + `spotlessCheck` on every PR.
