---
id: FEAT-005
slug: auth-signup
epic: 002-auth
milestone: 002-auth
sdd: SDD-001
frd: F1
status: planned
depends-on: []
blocks: [007-auth-signin]
date: 2026-06-07
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
