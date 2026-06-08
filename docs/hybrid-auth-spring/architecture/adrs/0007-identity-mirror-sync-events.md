# ADR-0007 — Identity-mirror synchronization: provision-before-authz + event-driven updates

- **Status:** Accepted
- **Date:** 2026-06-08
- **Milestone / Sprint:** 3 (resource)
- **Extends:** ADR-0006 (create-only mirror; this pins the *timing* and the *update transport* it left open)

## Context

ADR-0006 established that `app.users` is a **create-only** mirror of auth identity, provisioned
opportunistically on authenticated requests, with post-creation updates deferred. Running the full
stack end-to-end (the 2026-06-08 integration report) surfaced two specifics worth recording as
decisions rather than leaving implicit:

1. **When does provisioning run?** The `MirrorSyncInterceptor` provisions from the verified JWT claims
   in `preHandle` — i.e. **after authentication but before the controller and before
   ownership-authorization**. Observed consequence: a user who makes only *denied* (404) cross-user
   calls still gets their own mirror row created (in testing, `bob` got an `app.users` row after a
   404'd read of `alice`'s project).
2. **How do post-creation changes propagate?** ADR-0006 said "deferred" without committing to a
   transport. We want the direction on record so a future contributor doesn't reach for the wrong one.

## Decision

**Provisioning timing — provision-before-authz, accepted as-is.** The mirror row is keyed by the
caller's *own* verified `sub`; creating it before authorization leaks nothing (a user can always learn
their own identity) and creates no domain resource — it is an identity cache entry, not a grant. So
provisioning on every authenticated request, ahead of the ownership check, is **safe and intended** at
this tier. Tightening it to "provision only on a successful/owning operation" is a deliberate
**non-goal** for now; if ever wanted (e.g. to keep `app.users` strictly to users who own data), it is a
localized change in the interceptor/service and would be its own ADR.

**Update transport — event-driven is the chosen direction; callback is the documented alternative.**
To keep `auth.users` and the `app.users` mirror in step *after* creation (email/name changes, future
fields), the auth-service will **emit domain events** (`user.created` / `user.updated`) that the
resource-service (and any other consumer) subscribes to and applies to its local mirror. This is
**preferred over a callback** (auth POSTing to each backend) because:

- it keeps the issuer **ignorant of its consumers** — auth points at no one (SDD-001 §1), consumers
  point at auth; adding a backend needs no change to auth;
- it decouples availability — a consumer being down doesn't fail an auth-side write; the event is
  redelivered;
- it generalizes to N consumers without N callback configs.

A **callback** (auth → backend webhook) remains the documented lesser alternative: simpler to reason
about for a single consumer, but it couples the issuer to every backend and needs per-target retry +
idempotency + auth-of-the-callback handling. **Neither is implemented at this tier** — the MVP proves
creation-time sync only; this ADR records the direction so the deferred F-sync (SDD-002 §8) has a
decided shape. When built, "events" implies at-least-once delivery + idempotent apply on the consumer,
and an outbox on the auth side if event loss becomes unacceptable (playbook promotion trigger).

## Alternatives considered

- **Upsert every identity field on every request** — rejected (ADR-0006 §alt-a): writes on every
  request, can't carry `name` (absent from the token), and hides the propagation question this ADR
  answers.
- **Callback as the primary transport** — rejected as primary (couples issuer→consumers; violates the
  one-way boundary); kept as the documented fallback.
- **Provision only on owning/successful operations** — rejected for now (adds branching for no security
  gain; the row is a self-keyed identity cache). Left as a future option.

## Consequences

- **Positive:** the provisioning timing is a recorded, reasoned choice, not an accident; the sync
  evolution has a decided direction (events) so future work doesn't relitigate it; the auth→app boundary
  stays one-way.
- **Negative / follow-up:** `app.users` may hold rows for users who own no data (benign); identity
  fields can drift until the event path ships (OQ-007); building events introduces a broker + outbox /
  idempotency concerns. The resource-service must treat its mirror as eventually-consistent on
  non-identity-key fields (ownership, keyed by the immutable `sub`, is never affected).

## References

- `0006-user-identity-sync.md` (the create-only decision this pins down), `0003-database-per-service-isolation.md`,
  `../sdds/sdd-tasks.md` §4 invariant 3 / §8 F2 + F-sync, `../sdds/sdd-auth.md` §1 (auth points at no consumer),
  `../srs+sad.md` §2.7 (cross-service identity synchronization), `../../open-questions.md` OQ-007.
- Evidence: the 2026-06-08 full-stack integration test (mirror provisioned JIT, create-only; `bob`
  provisioned before a 404).
