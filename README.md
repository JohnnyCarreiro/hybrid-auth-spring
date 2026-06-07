# Hybrid Auth Spring

A production-shaped reference for **distributed authentication in the Spring ecosystem**. A dedicated
**auth-service** issues hybrid credentials — a server-side session (refresh) plus a short-lived
**RS256 JWT** access token — and publishes its public key via **JWKS**. A separate **resource-service**
verifies those JWTs **locally against the JWKS — no shared secret** — and authorizes by ownership.

> Status: **bootstrap**. The Gradle multi-module skeleton, isolated databases, and runtime baseline are
> in place; the auth flows (sign-up/sign-in, refresh rotation + reuse-detection, JWKS) and the
> projects/tasks domain land in the capability epics. Design lives in
> [`docs/hybrid-auth-spring/`](docs/hybrid-auth-spring/) (SRS+SAD, SDDs, ADRs).

## Stack

Java 21 · Spring Boot 3.5 · Spring Security 6 · **Jetty** · Spring Data JPA + **Flyway** · PostgreSQL ·
Redis · Gradle (Kotlin DSL, multi-module) · JUnit 5 + Testcontainers · Docker Compose.

Two isolated databases (one per service, no cross-DB FK — ADR-0003): `auth` (auth-service) and `app`
(resource-service).

## Quickstart

```sh
# Full stack (Postgres + Redis + both services), reproducible from a clean checkout:
just docker-up          # or: make docker-up
just health             # both /health → {"status":"UP"}
just docker-down

# Fast dev loop (host JDK 21 via your toolchain; infra in Docker):
just dev-run            # both services via bootRun (hot reload) + infra
just dev-auth           # one at a time: dev-auth / dev-resource
```

`just` (or `make`) with no target lists every recipe. Ports: `AUTH_PORT=3333`, `RESOURCE_PORT=3334`
(env; `3000` is reserved for a frontend).

## Contributing / dev setup

```sh
just hooks-install      # downloads lefthook locally (./.tools) + wires git hooks (no global, no npm)
```

- **Branching** (small tier): `feat/<NNN>-<slug>` → merge into `epic/<NNN>-<slug>` → **PR to `dev`**;
  `dev → main` is the release (tagged). `main` and `dev` are protected (PR-only; `main` is no-bypass).
- **Commits**: Conventional Commits. A local `commit-msg` hook checks the format; CI validates the PR
  title (which becomes the squash commit). Formatting is **google-java-format** via Spotless
  (`just fmt` / checked in CI).
- CI (`.github/workflows/ci.yml`) runs build + tests (Testcontainers) + `spotlessCheck` on every PR.
