---
id: EPIC-002
slug: auth
type: capability
status: active
owner: Johnny Carreiro
sdd: SDD-001
target_window: 2026-06
milestone: 002-auth
roadmap_cards:
  - 005-auth-signup
  - 006-auth-jwks
  - 007-auth-signin
  - 008-auth-me
  - 009-auth-refresh-rotation
  - 010-auth-signout
sprints: []
related_decisions:
  - ADR-0002 — auth stack (hand-built RS256 issuer)
  - ADR-0003 — database-per-service isolation
exits_with:
  - SDD-001 §8 F1–F6 acceptance met
  - sign-up / JWKS / sign-in / /me / rotation+reuse / sign-out working end-to-end
  - Testcontainers integration tests for the auth-critical flows
  - released v0.2.0
---

# EPIC-002 — auth

Milestone: [[../01-milestones/002-auth|MILESTONE-002]] (1:1). Tactical bible: [[../../architecture/sdds/sdd-auth|SDD-001]].
Features (build order = SDD §8 order): [[../03-features/005-auth-signup|FEAT-005 · F1 sign-up]],
[[../03-features/006-auth-jwks|FEAT-006 · F2 JWKS/keys]], [[../03-features/007-auth-signin|FEAT-007 · F3 sign-in]],
[[../03-features/008-auth-me|FEAT-008 · F4 /me]], [[../03-features/009-auth-refresh-rotation|FEAT-009 · F5 rotation ★]],
[[../03-features/010-auth-signout|FEAT-010 · F6 sign-out]].

## Why

The bootstrap skeleton runs; now the auth-service earns its name — issue and rotate hybrid credentials
(server-side refresh session + short-lived RS256 JWT) and publish JWKS, idiomatically in Spring Security 6.
This epic is the system's reason to exist ([[../../architecture/sdds/sdd-auth|SDD-001]]).

## Outcome

A user can sign up, sign in (hybrid creds), call a protected route (`/me`), rotate the refresh token with
reuse-detection (family revocation — the centerpiece), and sign out; the auth-service publishes its JWKS and
the private key never leaves it.

## Scope (in)

- SDD-001 §8 **F1–F6**: sign-up, signing keys/JWKS, sign-in, `/me`, refresh rotation + reuse-detection, sign-out.
- `auth` DB tables `users` / `sessions` / `jwks` (SDD §2.1); Flyway migrations per table.
- auth-service security filter chain; Argon2id passwords; Nimbus RS256 issuer.

## Out of scope

- resource-service domain (projects/tasks) and its JWKS-based JWT validation → next epic.
- OAuth/social login, RBAC, rate limiting, email-verification flow → Phase 2.

## Exits with

- [x] F1 sign-up · F2 JWKS/keys · F3 sign-in · F4 `/me` · F5 rotation+reuse ★ · F6 sign-out — all SDD §8 acceptance met.
- [x] Auth-critical flows covered by Testcontainers integration tests (ADR-0001).
- [x] `feat/<NNN> → epic/002-auth` merges done (six features); **epic → `dev` PR pending human validation** (§16.3).
- [ ] Released: `dev → main` tagged `v0.2.0`.

## Related decisions

- ADR-0002 — hand-built RS256 issuer on Spring Security 6 (the mechanics live in SDD-001).
- ADR-0003 — database-per-service isolation (the `auth` DB).

## Risks / open questions

- OQ-005 (UUID v7 generator: library vs Hibernate) — decide while building F1.
- Redis is optional at MVP (cache only) — promote to required only if a hot-path measurement justifies it (SDD §6).

## Progress log

2026-06-07 — Planned & activated after the `v0.1.0` release. SDD-001 §8 reordered to build sequence (signing
keys before sign-in). Cards FEAT-005..010 created. First build: FEAT-005 (F1 sign-up).

2026-06-08 — All six features (F1–F6) built and merged into `epic/002-auth`, each on its own `feat/<NNN>`
branch with a closing retro; OQ-005 resolved (uuid-creator, domain-generated). Full multi-module
`./gradlew build` green — auth-service 38 tests incl. the JWKS grace-window verify and the concurrent
FOR-UPDATE rotation race. **Pending human validation (§16.3) before the epic → `dev` PR and the `v0.2.0`
release.** Notable: the reuse-detection revoke runs inline under `noRollbackFor` (a `REQUIRES_NEW` revoke
self-deadlocks against the rotation's own row lock); the auth-service is its own resource server for `/me`
(local JWKS decode, no self-HTTP); recent Docker Engines need `DOCKER_HOST` + `api.version` for Testcontainers
(handled in the root build).
