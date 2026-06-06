# ADR-0001 — Testing stack: JUnit 5 + Mockito (+ Testcontainers for integration)

- **Status:** Accepted
- **Date:** 2026-06-05
- **Milestone / Sprint:** 0 (bootstrap)

## Context

The project needs a testing approach decided up front, since the auth-service carries the
security-critical logic (password hashing, RS256 signing, refresh rotation + reuse-detection) where
tests are the safety net, not an afterthought. The stack is Java 21 + Spring Boot 3.5, so the choice
should be idiomatic, well-supported by Spring Boot's test starter, and runnable from Gradle per module.

Two layers of testing are in scope:
- **Unit** — domain/use-case logic in isolation, collaborators mocked.
- **Integration** — auth-critical flows against real Postgres + Redis (session rotation, reuse-detection,
  JWKS issuance/verification) where in-memory fakes would not exercise the real behavior.

## Decision

We will use **JUnit 5 (Jupiter)** as the test framework and **Mockito** for mocking collaborators in
unit tests. For integration tests that need real infrastructure, we will use **Testcontainers**
(Postgres + Redis). All three come together via Spring Boot's `spring-boot-starter-test` (JUnit 5 +
Mockito bundled) plus the Testcontainers JUnit 5 extension; tests run per module through Gradle
(`./gradlew :auth-service:test`).

## Alternatives considered

- (a) **JUnit 4** — rejected: legacy; JUnit 5 is the default in current Spring Boot, with better
  parameterized tests, extensions, and nested test support.
- (b) **Spock (Groovy)** — rejected: expressive, but adds a Groovy toolchain and a second language to a
  Java-focused showcase; less idiomatic as a "Spring reference".
- (c) **In-memory / embedded fakes for integration (H2, embedded Redis)** — rejected for the auth-critical
  paths: H2 ≠ Postgres semantics (row locking, `FOR UPDATE` used by reuse-detection), and embedded Redis
  drifts from real behavior. Testcontainers runs the real engines. Lightweight unit tests still use plain
  fakes/mocks — no container needed.

## Consequences

- **Positive:** idiomatic, zero-friction setup via the Spring Boot test starter; unit tests stay fast
  (no I/O); integration tests are faithful (real Postgres/Redis), which matters most exactly where the
  risk is (token rotation/reuse-detection, JWKS).
- **Negative / follow-up:** Testcontainers requires a running Docker daemon (local + CI) and adds
  startup latency to the integration suite — mitigated by reusing containers across a test class and
  keeping the integration set focused on auth-critical flows. The Java CI workflow (OQ-002) must provide
  Docker. Definition of Done already requires happy-path + one negative case per feature (see AGENTS.md).

## References

- `../srs+sad.md` §2.3 (Dependencies) and §1.3 (NFR — testability without a front-end).
- `../playbook/playbook-base.md` §15 (Testing) and §10.5 (negative-case coverage).
- `open-questions.md` OQ-002 (Java CI must provide Docker for Testcontainers).
