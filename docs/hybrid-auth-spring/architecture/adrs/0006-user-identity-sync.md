# ADR-0006 — User identity sync: create-only mirror, deferred event-based updates

- **Status:** Accepted
- **Date:** 2026-06-08
- **Milestone / Sprint:** 3 (resource)
- **Extends:** ADR-0003 (database-per-service: `app.users` is a mirror of auth identity)

## Context

A user exists in two databases (ADR-0003): `auth.users` is the source of truth; `app.users` is a local
**mirror** so the resource-service can own a real `projects.owner_id` foreign key without reaching
across the boundary (no cross-DB FK/query). ADR-0003 said the mirror is "populated opportunistically on
authenticated requests … eventual consistency bounded by the access-token TTL", but left the exact
mechanism — and crucially, *whether and how updates propagate* — undecided.

The access token carries `sub` (the user id), `email`, and `email_verified`, but **not** `name`. So the
mirror can be populated from the token for id/email, but `name` and any later identity changes need a
separate path.

## Decision

**Create-only, JIT provisioning from the verified token; updates deferred.**

- **Provision on first sight (JIT).** On every authenticated request, after the JWT is verified,
  `MirrorSyncInterceptor` reads the claims (`sub`, `email`, `email_verified`) and runs
  `INSERT INTO users (…) VALUES (…) ON CONFLICT (id) DO NOTHING`. The row exists by the time a
  `POST /projects` needs the `owner_id` FK. Idempotent and concurrency-safe (one row under a race).
- **Create-only.** `ON CONFLICT DO NOTHING` means an existing row is **never updated** from a later
  token. The mirror reflects identity *as of first sight*; subsequent auth-side changes do not
  overwrite it. Ownership is keyed by the immutable `sub`, so staleness never affects authorization —
  only the convenience copy of email/name.
- **Updates are a documented, deferred capability (F-sync).** The **recommended** propagation path is an
  **auth-emitted event** (`user.updated`) that the resource-service consumes and applies to its mirror —
  it keeps the issuer ignorant of its consumers. An **outbound callback** from auth to each backend is
  the **less-favoured** alternative (it couples the issuer to every consumer and needs ret/idempotency
  bookkeeping). **Neither is built at this tier** — the MVP proves creation-time sync; update
  propagation is Phase 2.
- **`name` at creation.** Not in the token, so the mirror's `name` is left null at provisioning and
  filled by the same future sync path.

## Alternatives considered

- **(a) Upsert every field on every request** (`ON CONFLICT DO UPDATE`) — keeps the mirror within one
  token-TTL of auth for free, matching ADR-0003's literal "eventual consistency bounded by the TTL".
  Rejected at this tier: it writes on every request, can't carry `name` (absent from the token), and
  silently masks the "how do updates really propagate" question the showcase should answer explicitly.
- **(b) Push on creation from auth (callback at sign-up)** — auth POSTs the new user to each backend.
  Rejected as the *primary* mechanism: couples the issuer to its consumers and fails the "auth points at
  no one" boundary (SDD-001 §1); kept as the documented lesser alternative for updates.
- **(c) No mirror — store `owner_id` as a bare value, no FK** — simplest, but throws away referential
  integrity within the `app` DB and a place to cache email/name. Rejected: the mirror is cheap and makes
  the app domain self-consistent.

## Consequences

- **Positive:** no coupling from auth to the resource-service for creation; the FK target always exists
  before it's needed; authorization is never affected by mirror staleness (keyed by immutable `sub`);
  the update path is named and reasoned about rather than hand-waved.
- **Negative / follow-up:** email/name in the mirror can drift after the user changes them in auth until
  F-sync ships (tracked as OQ-007); `name` is null until then. Building F-sync introduces an event bus
  (or a callback contract) and the at-least-once/idempotency concerns that come with it.

## References

- `0003-database-per-service-isolation.md` (the mirror this refines), `../sdds/sdd-tasks.md` §2/§4
  invariant 3/§8 F2 + F-sync, `../sdds/sdd-auth.md` §1 (auth points at no consumer),
  `../../roadmap/03-features/012-resource-user-mirror.md`.
