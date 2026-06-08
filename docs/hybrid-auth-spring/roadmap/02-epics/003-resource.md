---
id: EPIC-003
slug: resource
type: capability
status: active
owner: Johnny Carreiro
sdd: SDD-002
target_window: 2026-06
milestone: 003-resource
roadmap_cards:
  - 011-resource-jwt-verifier
  - 012-resource-user-mirror
  - 013-resource-projects-crud
  - 014-resource-tasks-crud
sprints: []
related_decisions:
  - ADR-0005 — resource-server hand-built JWKS verifier (refines ADR-0002)
  - ADR-0006 — user identity sync (create-only mirror; deferred event updates; extends ADR-0003)
exits_with:
  - SDD-002 §8 F1–F4 acceptance met
  - JWT verify (local, JWKS, refetch-on-rotation) / user mirror / projects CRUD / tasks CRUD working end-to-end
  - Testcontainers integration tests + a direct verifier unit test
  - released v0.3.0
---

# EPIC-003 — resource

Milestone: [[../01-milestones/003-resource|MILESTONE-003]] (1:1). Tactical bible: [[../../architecture/sdds/sdd-tasks|SDD-002]].
Features (build order = SDD §8 order): [[../03-features/011-resource-jwt-verifier|FEAT-011 · F1 JWT verifier ★]],
[[../03-features/012-resource-user-mirror|FEAT-012 · F2 identity mirror]], [[../03-features/013-resource-projects-crud|FEAT-013 · F3 projects CRUD]],
[[../03-features/014-resource-tasks-crud|FEAT-014 · F4 tasks CRUD]].

## Why

The auth-service is the star, and it is done. This epic makes the **other half** real: a second service
that verifies the hybrid credential with **no shared secret**, mirrors identity across the database
boundary (ADR-0003), and protects an ordinary MVC CRUD domain by ownership. It is the proof that "verify
locally via JWKS, scale out backends without touching the issuer" actually holds (SRS+SAD §2.1).

## Outcome

A holder of an access JWT can CRUD their own projects and tasks; another user's resources are invisible
(404); a forged/expired token is 401; the resource-service fetches the JWKS once, caches it, and picks up
a key rotation automatically. The mirror is provisioned on first sight, create-only. No token is ever
issued or rotated here.

## Scope (in)

- SDD-002 §8 **F1–F4**: local JWKS verifier (★), identity mirror, projects CRUD, tasks CRUD.
- `app` DB tables `users` (mirror) / `projects` / `tasks` (SDD §2.1); Flyway V2–V4.
- resource-service stateless security chain; ownership-based authorization (404 over 403).

## Out of scope

- Token issuance / refresh rotation (auth-service + BFF) — never here (SDD-002 §4 invariant 6).
- **F-sync** — identity update propagation (event preferred; callback the lesser alternative) →
  deferred, OQ-007.
- OpenAPI/Swagger (REQ-012) → OQ-008; OAuth/social, RBAC, rate limiting → Phase 2.

## Exits with

- [ ] F1 verifier ★ · F2 mirror · F3 projects CRUD · F4 tasks CRUD — all SDD §8 acceptance met.
- [ ] Resource-critical flows covered by Testcontainers integration tests + the verifier unit test (ADR-0001).
- [ ] Feature merges done; epic squash-merged to `dev` via PR (§16.3).
- [ ] Released: `dev → main` tagged `v0.3.0`.

## Related decisions

- ADR-0005 — hand-built singleton JWKS verifier wired into Spring's resource-server chain (refines ADR-0002).
- ADR-0006 — create-only mirror provisioning; update propagation deferred to an event (extends ADR-0003).

## Risks / open questions

- OQ-007 (identity update propagation: event vs callback) — deferred; the mirror is create-only meanwhile.
- OQ-008 (Swagger not wired on either service) — out of scope for this epic.

## Progress log

2026-06-08 — Planned & activated after the `v0.2.0` release. SDD-002 authored (subdomain-type: core);
ADR-0005 (verifier, refines ADR-0002) and ADR-0006 (create-only sync, extends ADR-0003) accepted. All four
features (F1–F4) **built and green** in one pass: `JwksTokenVerifier` (in-memory cache + refetch-once,
wired as the `JwtDecoder`), create-only JIT mirror via `MirrorSyncInterceptor`, owner-scoped projects +
tasks CRUD (404-over-403), Flyway V2–V4. `./gradlew :resource-service:test` green (verifier unit test
against a JDK-`HttpServer` JWKS stub: rotation/forged/expired; CRUD + ownership + mirror create-only on
Testcontainers Postgres); `spotlessCheck` green across modules. **Pending human validation (§16.3) before
the per-feature branches / epic → `dev` PR and the `v0.3.0` release.** Notable: the verifier deliberately
hand-builds what Spring's `jwk-set-uri` does internally, for symmetry with the hand-built issuer and to
keep the mechanism visible (ADR-0005); ownership uses 404 not 403 to avoid confirming others' resources;
tasks derive ownership through the parent project (no `owner_id` on a task).
