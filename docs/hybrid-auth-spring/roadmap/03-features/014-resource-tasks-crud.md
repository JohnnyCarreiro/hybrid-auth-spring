---
id: FEAT-014
slug: resource-tasks-crud
epic: 003-resource
milestone: 003-resource
sdd: SDD-002
frd: F4
status: done
depends-on: [013-resource-projects-crud]
blocks: []
date: 2026-06-08
closed: 2026-06-08
---

# FEAT-014 — resource tasks CRUD (F4)

Realizes [[../02-epics/003-resource|EPIC-003 — resource]] · [[../01-milestones/003-resource|MILESTONE-003]].
Build of **[[../../architecture/sdds/sdd-tasks|SDD-002]] §8 F4** — Tasks CRUD (ownership derived through the
project). Acceptance + tasks are the SDD's. Last feature of the epic → closes EPIC-003 → `v0.3.0`.

## Intent

CRUD over tasks nested under a project (`/projects/{projectId}/tasks`) and addressed directly
(`/tasks/{id}`). A task carries **no** owner of its own — ownership is derived through the parent project,
so a task is reachable exactly when its project is (SDD-002 §4 invariant 2).

## Sequence

- **Depends on:** F3 (the parent project).
- **Blocks:** —
- Last feature of EPIC-003; closes the epic → `dev → main` → `v0.3.0`.

## Done when

SDD-002 §8 F4 acceptance met (create/list under an owned project; read/update/delete an owned task; a task
under an unowned/unknown project → 404; another user's task → 404; unknown status → 400; status defaults
TODO) + feature DoD. On close: epic `exits_with` checked → `v0.3.0`.

## Build (2026-06-08)

**What landed.** `domain/task/Task` + `TaskStatus` (`TODO|DOING|DONE`, `@Enumerated(STRING)`) +
`TaskNotFoundException`; `TaskRepository` (`findByProjectIdOrderByCreatedAtDesc`); `TaskService` —
**derived** authz: every op first proves project ownership via `ProjectService`, so a task under an
unowned/unknown project → 404 (`PROJECT_NOT_FOUND` on project-rooted routes, `TASK_NOT_FOUND` on task
routes); `TaskController` + `TaskRequest`/`Response`; migration `V4__tasks.sql`.

**Tests.** `TaskCrudIntegrationTest`: create (default TODO) / list / update-status / delete under an owned
project; cross-user create under another's project → 404 `PROJECT_NOT_FOUND`; cross-user task read → 404
`TASK_NOT_FOUND`; unknown status → 400. Green.

**Status.** Built and green; **pending human validation (§16.3)** before the epic PR + `v0.3.0` release.
