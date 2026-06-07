# ADR-0004 — Runtime baseline: Jetty embedded server + Flyway migrations

- **Status:** Accepted
- **Date:** 2026-06-07
- **Milestone / Sprint:** 1 (bootstrap)

## Context

Before domain code, the bootstrap needs a "production-shaped" runtime: a chosen embedded HTTP server
and a database-migration tool for the two isolated databases (ADR-0003). Spring Boot's default server
is **Tomcat** and no migration tool is wired yet. Both are bootstrap-level decisions (FEAT-003).

## Decision

**Embedded server → Jetty.** Replace the default Tomcat in both services: exclude
`spring-boot-starter-tomcat` from the web starter and add `spring-boot-starter-jetty`.

- Lighter memory footprint than Tomcat — relevant running two services + Postgres + Redis locally.
- **Actively maintained** (Eclipse/Webtide), unlike Undertow's slower cadence.
- Solid **Java 21 virtual-thread** support (matters for a Java 21 project; stronger than Undertow's).
- First-class in Spring Boot; the swap is one line per module and reversible.

**Migrations → Flyway**, per service, plain versioned SQL. Each service runs Flyway against **its own**
database (auth-service → `auth`, resource-service → `app`); migrations live in each module under
`src/main/resources/db/migration`. Bootstrap stands up the **pipeline + a baseline**; the domain tables
(`users`/`sessions`/`jwks`, `projects`/`tasks`) land with their own features.

## Alternatives considered

- **Server — keep Tomcat:** default and the most Spring-tested, but heavier; we want the lighter runtime
  and to demonstrate a clean server swap. **Undertow:** lighter too, but slower maintenance and weaker
  virtual-thread story than Jetty — rejected in favour of Jetty.
- **Migrations — Liquibase:** DB-agnostic changelogs (XML/YAML/SQL). Rejected: for a Postgres-only
  showcase, Flyway's plain-SQL versioned migrations are simpler and **read like the DDL in the SDD**;
  Liquibase's abstraction layer buys nothing here.

## Consequences

- **Positive:** lighter runtime; transparent, reviewable SQL migrations per isolated DB; the server is a
  one-line, reversible choice; migration history (`flyway_schema_history`) gives an auditable schema trail.
- **Negative / follow-up:** the services now need a datasource to boot (datasource autoconfig), so the dev
  loop requires Postgres up (`docker-up-infra`) once this lands; two Flyway configs (one per service/DB);
  Jetty is slightly less "default-tested" than Tomcat (low risk, fully supported). The `flyway-database-postgresql`
  module is required on Flyway 10+.

## References

- `../srs+sad.md` §2.3 (dependencies), `0003-database-per-service-isolation.md` (the two databases this
  migrates), `../../roadmap/03-features/003-runtime-baseline.md` (FEAT-003).
