# ADR-0003 — Database-per-service: isolate auth and app data stores

- **Status:** Accepted
- **Date:** 2026-06-07
- **Milestone / Sprint:** 1 (bootstrap)

## Context

Authentication and the application are **separate bounded contexts** (`srs+sad.md` §2): the auth-service
owns identity/credentials/sessions/keys; the resource-service owns projects/tasks. The system exists to
demonstrate **distributed** authentication, so the two sides should look like **two independent systems**
that happen to share an auth contract (the JWKS + JWT), not one app with an auth module.

The production reference this project ports isolated the two contexts by **schema** (`auth.*` / `app.*`)
inside a single database, with no foreign keys crossing the boundary. We go one step further to make the
separation unambiguous in the reference.

## Decision

Run **two isolated databases**, one per service:

- **`auth`** — owned by the auth-service: `users` (identity source of truth), `sessions`, `jwks`.
- **`app`** — owned by the resource-service: `users` (a *mirror* of a subset of auth identity, keyed by
  the auth user id), `projects`, `tasks`.

Rules:
- **No cross-database foreign keys or queries.** `app.users.id` references the auth user by value only;
  the link is logical, never a DB constraint.
- **`auth.users` is the identity source of truth; `app.users` is a cache** of a few fields (e.g. email),
  populated opportunistically on authenticated requests — eventual consistency bounded by the access-token
  TTL (the hybrid-auth sync pattern).
- **Each service connects only to its own database** with its own credentials (`auth_user` → `auth`,
  `app_user` → `app`); neither can read the other's store.

At this tier both databases live on **one Postgres instance** (lighter for local/showcase). The topology is
instance-agnostic: because there are no cross-DB FKs or queries, promoting `app` to a second Postgres
server later is a **connection-string change**, nothing more.

## Alternatives considered

- **(a) One database, one schema (shared tables)** — rejected: couples the two contexts and invites
  accidental cross-context joins; the opposite of what the showcase demonstrates.
- **(b) One database, two schemas (`auth.*` / `app.*`) — the reference's approach** — rejected *here*: it
  isolates names but still allows cross-schema FKs/queries within one DB. Two databases enforce the
  boundary structurally.
- **(c) Two Postgres instances (one per service)** — deferred: strongest isolation but heavier locally.
  The two-database split already gives the property we care about (no cross-DB FK/query); moving to two
  instances later is just connection strings (see Decision).

## Consequences

- **Positive:** clean ownership and a structurally-enforced boundary; faithful to "distributed"; forces the
  realistic auth→app sync (no shared tables to lean on); trivially promotable to separate servers.
- **Negative / follow-up:** `app.users` duplicates a few `auth.users` fields (eventual consistency); two
  datasources + two credential sets to configure (wired when the datasources land, F2+); integration tests
  spin up both databases (Testcontainers, ADR-0001); a per-service migration tool (Flyway/Liquibase) is a
  new open question. Schema naming inside each DB (`public` vs named `auth`/`app` schema) decided when the
  first migrations land.

## References

- `../srs+sad.md` §2 (architecture, bounded contexts) and §2.4 (dataflow).
- `../sdds/sdd-auth.md` §6 (auth-service owns the `auth` database).
- `0002-auth-stack-handbuilt-rs256-issuer.md` (the JWT/JWKS contract that is the *only* coupling across the boundary).
