---
id: EPIC-000
slug: scaffold
type: infra
status: done
owner: Johnny Carreiro
sdd:
target_window: 2026-06
milestone: 000-scaffold
roadmap_cards:
  - 000-docs-foundation
sprints: []
related_decisions:
  - ADR-0001 — testing stack
  - ADR-0002 — auth issuer stack
  - SDD-001 — authentication
exits_with:
  - requirements + architecture documented
  - auth SDD authored
  - stack + testing decisions recorded
  - board stood up
  - repository initialized and pushed
---

# EPIC-000 — scaffold

Milestone: [[../milestones/000-scaffold|MILESTONE-000]] (1:1). Feature: [[../features/000-docs-foundation|FEAT-000]].

## Why

Before writing code, the project needs its documentation and architecture foundation: what we're
building (SRS+SAD), how the auth domain works (SDD-001), the decisions behind the stack (ADRs), and the
management trail. This is a `small`-tier reference, so the design is pinned up front rather than
discovered mid-build.

## Outcome

The docs vault is complete and navigable — requirements, threat model, the auth SDD, two ADRs,
methodology + playbook, and a milestone/epic board — and the repository is initialized (branch `dev`
default + `main`) and pushed to GitHub.

## Scope (in)

- SRS+SAD, threat model, open questions.
- Auth domain SDD (SDD-001).
- ADR-0001 (testing stack), ADR-0002 (auth issuer stack).
- Methodology + playbook, roadmap/epics/board structure.
- Repository init, commit hygiene, first push.

## Out of scope

- Any application code (→ EPIC-001 bootstrap).

## Exits with

- [x] Requirements + architecture documented (SRS+SAD, threat model).
- [x] Auth SDD authored (SDD-001).
- [x] Stack + testing decisions recorded (ADR-0001, ADR-0002).
- [x] Milestone/epic board stood up.
- [x] Repository initialized and pushed (dev default + main).

## Related decisions

- ADR-0001 — testing stack: JUnit 5 + Mockito (+ Testcontainers).
- ADR-0002 — auth issuer stack: hand-built RS256 issuer on Spring Security 6.
- SDD-001 — authentication (domain bible).

## Risks / open questions

- OQ-001 (Java playbook), OQ-002 (CI), OQ-003 (lint), OQ-005 (UUID v7) — carried into EPIC-001.

## Progress log

2026-06-05 — Scaffold complete; repository initialized (dev + main) and pushed. Epic closed.
