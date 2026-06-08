---
id: SDD-002
slug: tasks
title: Resource domain (projects, tasks, identity mirror)
subdomain-type: core
status: draft
date: 2026-06-08
---

# SDD-002 — Resource domain (projects, tasks, identity mirror)

The tactical-design bible of the **resource** domain: how the resource-service verifies access
tokens against the auth-service JWKS, mirrors auth identity locally, and serves ownership-scoped CRUD
over projects and tasks. The two cross-boundary decisions behind it are
`../adrs/0005-resource-server-jwks-verifier.md` (how it verifies) and
`../adrs/0006-user-identity-sync.md` (how it mirrors identity); this document is the mechanics. The
auth domain it consumes is `sdd-auth.md` (SDD-001); the only seam between them is the public JWKS.

## 1. Domain / context

The resource-service owns the **app** domain: **projects** and **tasks** (each owned by a user) plus
a **local identity mirror** (`app.users`) of a subset of auth identity. It lives in the
`resource-service/` Gradle module with its own Postgres database (`app` — ADR-0003).

It depends on the auth domain through **one** seam only: the public JWKS endpoint. The reference is
one-way — the resource-service fetches `/.well-known/jwks.json`, caches the public keys, and verifies
tokens locally; it never shares a database, a secret, or a per-request call with auth, and it
**never issues or rotates tokens** (refresh rotation is the BFF↔auth concern — SRS+SAD §2.1).
Nothing here points back at the auth domain.

## 2. Aggregates / domain types

- **AppUser (the identity mirror)** — `id` (= the auth user id / access-token `sub`; **not** minted
  here), `email`, `emailVerified` (bool), `name` (nullable — not carried by the token), `createdAt`,
  `updatedAt`. A *cache* of auth identity, not a source of truth (ADR-0003). Provisioned **create-only**
  on the first authenticated request (ADR-0006); see §4 invariant 3.
- **Project — the owned root.** `id` (UUID v7), `ownerId` (= auth `sub`; immutable), `name`,
  `description` (nullable), `createdAt`, `updatedAt`. Ownership is the authorization boundary (§4
  invariant 1).
- **Task — child of a project.** `id` (UUID v7), `projectId` (FK → projects), `title`, `description`
  (nullable), `status` (`TaskStatus`: `TODO | DOING | DONE`, default `TODO`), `createdAt`,
  `updatedAt`. Carries **no** owner of its own — ownership is derived through the project (§4
  invariant 2).

Schema DDL: see §2.1. `Project`/`Task` mint their own id via `IdMint` (UUID v7, as auth does — OQ-005);
`AppUser.id` is copied from the verified `sub`.

### 2.1 Schema

```sql
-- The identity mirror — keyed by the auth user id; no cross-DB FK to auth (ADR-0003).
CREATE TABLE users (
  id             uuid PRIMARY KEY,            -- = auth `sub`; mirrored, never minted here
  email          text NOT NULL,
  email_verified boolean NOT NULL DEFAULT false,
  name           text,                        -- not in the token; populated by a future sync
  created_at     timestamptz NOT NULL DEFAULT now(),
  updated_at     timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE projects (
  id          uuid PRIMARY KEY,
  owner_id    uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,  -- LOCAL FK (ADR-0003)
  name        text NOT NULL,
  description text,
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX projects_owner_id_idx ON projects(owner_id);

CREATE TABLE tasks (
  id          uuid PRIMARY KEY,
  project_id  uuid NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  title       text NOT NULL,
  description text,
  status      text NOT NULL DEFAULT 'TODO',   -- TaskStatus, mapped @Enumerated(STRING)
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX tasks_project_id_idx ON tasks(project_id);
```

## 3. Use cases / operations

| Operation | Input | Output | Errors |
|-----------|-------|--------|--------|
| `verifyAccessToken` *(filter)* | Bearer JWT | authenticated principal (`sub`) | 401 (missing/invalid/expired) |
| `ensureProvisioned` *(per request)* | verified claims (`sub`, `email`, `email_verified`) | void (create-only) | — |
| `createProject` | ownerId, name, description | Project | — |
| `listProjects` | ownerId | Project[] (owned) | — |
| `getProject` | ownerId, id | Project | `ProjectNotFound` |
| `updateProject` | ownerId, id, name, description | Project | `ProjectNotFound` |
| `deleteProject` | ownerId, id | void | `ProjectNotFound` |
| `createTask` | ownerId, projectId, title, description, status | Task | `ProjectNotFound` |
| `listTasks` | ownerId, projectId | Task[] | `ProjectNotFound` |
| `getTask` | ownerId, taskId | Task | `TaskNotFound` |
| `updateTask` | ownerId, taskId, title, description, status | Task | `TaskNotFound` |
| `deleteTask` | ownerId, taskId | void | `TaskNotFound` |

## 4. Invariants

Each gets at least one explicit test (ADR-0001).

1. **Project ownership.** Every project operation is scoped to `owner_id = <token sub>`. A finder is
   never plain `findById` in the service path — the owner is part of the query, so the check cannot be
   forgotten. `owner_id` is immutable (a project never changes hands).
2. **Derived task ownership.** A task is reachable exactly when the caller owns its parent project;
   the task carries no owner of its own, so it can never disagree with its project about who owns it.
3. **Create-only mirror.** The `app.users` row for a `sub` is inserted on first sight and **never
   updated** from a later token (`INSERT … ON CONFLICT DO NOTHING`) — synced on creation only
   (ADR-0006). Concurrency-safe: two simultaneous first-requests yield one row.
4. **Local, secret-free verification.** The access JWT is verified in-process against the auth-service's
   *public* JWKS (RS256 signature + expiry). No shared secret; no per-request call to auth on the happy
   path. Keys are cached in memory and refetched once on a verify miss (rotation), then 401 (ADR-0005).
5. **404 over 403 for ownership.** A request for a project/task that does not exist *or* is owned by
   another user is answered identically as 404, so the API never confirms another user's resource (§5).
6. **No token issuance.** The resource-service mints no tokens and rotates no refresh tokens — it only
   consumes and verifies. Refresh rotation lives at the BFF↔auth edge (SRS+SAD §2.1).

## 5. Errors

The resource error set and its HTTP mapping (the behavioral contract for the API surface):

| Variant | HTTP | When |
|---------|------|------|
| `ProjectNotFound` | 404 | project id unknown, **or** owned by another user, **or** a task route names an unowned project |
| `TaskNotFound` | 404 | task id unknown, **or** its parent project is owned by another user |
| `VALIDATION_FAILED` | 400 | bean-validation failure, or an unparseable body (e.g. unknown `TaskStatus`) |
| *(unauthenticated)* | 401 | missing/invalid/expired Bearer — produced by Spring Security's filter, never by the domain |

`ProjectNotFound`/`TaskNotFound` deliberately conflate "absent" and "not yours" (invariant 5).

## 6. Ports / external dependencies

- **Postgres** — the **`app`** database (source of truth for `projects`, `tasks`; cache for `users`),
  isolated from the auth store (ADR-0003), via Spring Data JPA. Owner-scoped finders + a native
  `INSERT … ON CONFLICT DO NOTHING` for mirror provisioning.
- **auth-service JWKS** — `GET /.well-known/jwks.json` over HTTP, read-only. Fetched by
  `JwksTokenVerifier` (Nimbus `JWKSet.load`), cached in memory, refetched once on a verify miss.
- **Nimbus JOSE** (`spring-security-oauth2-jose`) — RS256 signature verification (`DefaultJWTProcessor`
  + `JWSVerificationKeySelector`), wrapped in a `NimbusJwtDecoder` for Spring's default validators.
- **Spring Security 6** — configured as a stateless **resource server**; the hand-built verifier is the
  `JwtDecoder`. CSRF disabled by construction (stateless token API — SRS+SAD §2.5).

## 7. Behavioral API surface

All routes protected (`Authorization: Bearer <access JWT>`) except `/health`. Routes name domain
intentions (playbook §6.1).

| Method | Route | Use case | Aggregate |
|--------|-------|----------|-----------|
| GET | `/projects` | `listProjects` | Project |
| POST | `/projects` | `createProject` | Project |
| GET | `/projects/{id}` | `getProject` | Project |
| PUT | `/projects/{id}` | `updateProject` | Project |
| DELETE | `/projects/{id}` | `deleteProject` | Project |
| GET | `/projects/{projectId}/tasks` | `listTasks` | Task |
| POST | `/projects/{projectId}/tasks` | `createTask` | Task |
| GET | `/tasks/{id}` | `getTask` | Task |
| PUT | `/tasks/{id}` | `updateTask` | Task |
| DELETE | `/tasks/{id}` | `deleteTask` | Task |
| GET | `/health` | liveness | — |

## 8. Functionalities (child FRDs — absorbed at small tier)

One block per functionality: intent · acceptance · validation · Tasks (1–2 day units). **Ordered by
build/dependency sequence**: the verifier protects every route (must exist first); the mirror anchors
the `owner_id` FK (before projects can persist); projects precede tasks (FK parent).

### F1 — Local JWT verifier (JWKS, no shared secret)  *(centerpiece)*
- **Depends on:** the auth-service JWKS endpoint (SDD-001 §7).
- **Intent:** verify the Bearer access JWT locally against the auth-service public JWKS, caching keys in
  memory and refetching once on a verify miss (rotation), then rejecting with 401. A hand-built
  singleton `JwtDecoder`, symmetric with auth's hand-built issuer (ADR-0005, refines ADR-0002).
- **Acceptance:** a token signed by a served key verifies; after a key rotation a token under the new
  `kid` verifies on the next request (one refetch); a forged signature or an expired token → 401; the
  happy path does **not** refetch (in-memory cache); no shared secret anywhere.
- **Validation:** RS256 only; Nimbus default validators (exp/nbf); JWKS fetch timeouts bounded; a
  forged token costs at most one (cacheable) refetch.
- **Tasks:** `JwksTokenVerifier` (cache + refetch-once) · `SecurityConfig` (stateless resource server,
  all routes authed but `/health`) · unit test against an in-JVM JWKS stub (rotation, forged, expired).

### F2 — Identity mirror (create-only JIT sync)
- **Depends on:** F1 (a verified `sub` to mirror).
- **Intent:** keep a local `app.users` row per auth user so the app domain can own a real `owner_id`
  FK. Provision JIT on the first authenticated request from the token claims; **create-only**
  (ADR-0006). The update path (auth-emitted event preferred; outbound callback the lesser alternative)
  is documented and **deferred**.
- **Acceptance:** the first authenticated request for a `sub` inserts exactly one row from the claims; a
  later token with changed email/`email_verified` does **not** overwrite it; concurrent first-requests
  yield one row (no error).
- **Validation:** `INSERT … ON CONFLICT (id) DO NOTHING`; provisioning commits before the controller
  runs (FK satisfiable for `POST /projects`).
- **Tasks:** `AppUser` entity · `provisionIfAbsent` repository upsert · `UserMirror` service ·
  `MirrorSyncInterceptor` + `WebConfig` · integration test (provisions once; create-only on change).

### F3 — Projects CRUD (owner-scoped)
- **Depends on:** F1 (auth), F2 (the `owner_id` FK target).
- **Intent:** owner-scoped CRUD over `/projects`. Plain Spring MVC — the auth is the interesting part.
- **Acceptance:** create/list/get/update/delete scoped to the caller; another user's project → 404
  (not 403, invariant 5); list shows only owned projects; blank name → 400; no token → 401.
- **Validation:** `@NotBlank`/`@Size` at the edge; ownership enforced in `ProjectService` (404).
- **Tasks:** `Project` aggregate + migration V3 · `ProjectRepository` (owner-scoped finders) ·
  `ProjectService` · `ProjectController` + DTOs · `ResourceExceptionHandler` · integration tests
  (CRUD, cross-user 404, 401, 400).

### F4 — Tasks CRUD (ownership derived through the project)
- **Depends on:** F3 (the parent project).
- **Intent:** CRUD over tasks nested under a project (`/projects/{id}/tasks`) and addressed directly
  (`/tasks/{id}`); ownership derived through the project (invariant 2).
- **Acceptance:** create/list under an owned project; read/update/delete an owned task; a task under an
  unowned/unknown project → 404; another user's task → 404; unknown status → 400; status defaults to
  `TODO`.
- **Validation:** project ownership checked before every task op; `@Enumerated(STRING)` status.
- **Tasks:** `Task` + `TaskStatus` + migration V4 · `TaskRepository` · `TaskService` (derived authz) ·
  `TaskController` + DTOs · integration tests (CRUD, cross-user 404, 400).

### F-sync — Identity update propagation *(deferred — Phase 2)*
- **Intent:** propagate auth-side identity changes (email/name) to the mirror after creation. **Not
  built** at this tier (ADR-0006): preferred design is an auth-emitted `user.updated` event the
  resource-service consumes; an outbound callback from auth is the less-favoured alternative (couples
  the issuer to its consumers). Tracked as the follow-up to F2.

## 9. Open items

- `../../open-questions.md` **OQ-007** — identity update propagation (event vs callback): deferred per
  ADR-0006; revisit when a second consumer or a real freshness requirement appears.
- Swagger/OpenAPI (REQ-012) is not yet wired on either service (no springdoc dependency); add when the
  dev-inspection surface is prioritized — tracked alongside the auth-service gap.
- A `shared` module could host `IdMint` (duplicated verbatim in both services) — left duplicated until
  a second cross-cutting contract earns the module (playbook: introduce `shared` only when it pays).
