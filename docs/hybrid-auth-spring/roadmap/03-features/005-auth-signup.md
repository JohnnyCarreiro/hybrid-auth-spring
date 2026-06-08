---
id: FEAT-005
slug: auth-signup
epic: 002-auth
milestone: 002-auth
sdd: SDD-001
frd: F1
status: done
depends-on: []
blocks: [007-auth-signin]
date: 2026-06-07
closed: 2026-06-07
---

# FEAT-005 — auth sign-up

Realizes [[../02-epics/002-auth|EPIC-002 — auth]] · [[../01-milestones/002-auth|MILESTONE-002]].
Build of **[[../../architecture/sdds/sdd-auth|SDD-001]] §8 F1** — Email + password sign-up. Acceptance +
tasks are the SDD's (management → docs); this card tracks status/sequence.

## Intent

Register a user with email + password; store the password only as an **Argon2id** hash. No auto-login at MVP.

## Sequence

- **Depends on:** — (foundational; rides on the EPIC-001 runtime baseline: `auth` DB + Flyway).
- **Blocks:** F3 sign-in (needs `users`).
- First feature of the epic; also where **OQ-005** (UUID v7 generator) is decided.

## Done when

SDD-001 §8 F1 acceptance met (valid → 200 + user; duplicate → 409; weak → 422; no clear-text password row)
+ feature DoD (happy + ≥1 negative test, module `AGENTS.md` updated if the surface changed, CI green,
merged into `epic/002-auth` via PR).

## Retro (2026-06-07)

Shipped on `feat/005-auth-signup` (commit `ec4f312`), merged into `epic/002-auth`.

**What landed.** `User` aggregate owning its UUID v7 identity (`support/IdMint`, never the ORM) and born
consistent via `User.register`; `Email` value object (normalization); length-based `PasswordPolicy`;
Argon2id hashing (`spring-security-crypto` + BouncyCastle, hash-only persistence); `POST /auth/sign-up`
returning 200 + user. Typed `AuthException`/`AuthErrorCode` with a single RFC 7807 web edge. `V2__users.sql`.
Module `AGENTS.md` created. Acceptance proven by a Testcontainers IT (Argon2id hash persisted, case-folded
duplicate → 409, weak → 422, malformed → 400) plus `PasswordPolicy` unit cases.

**Decisions.** Resolved **OQ-005** → `uuid-creator`, generated in the domain. Established the module's
canonical patterns (domain-owned identity, infra-free domain, `support/error` mapping) for F2–F6 to follow.

**Friction.** Testcontainers vs. a very recent Docker Engine: docker-java pinged with API 1.32 (daemon
min 1.40). Fix lives in the root build — forward `DOCKER_HOST` + map `DOCKER_API_VERSION` to docker-java's
`api.version` system property (env alone is ignored). Documented in the module `AGENTS.md`.
