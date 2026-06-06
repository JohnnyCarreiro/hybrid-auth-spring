---
id: MILESTONE-000
slug: scaffold
title: Scaffold
window: 2026-06
status: done
date: 2026-06-05
---

# MILESTONE-000 — Scaffold

1:1 with EPIC-000 (at this tier each milestone groups exactly one epic, and takes its name).

## Goal

Stand up the project's documentation and architecture foundation before any code: what we're building
(SRS+SAD), how the auth domain works (SDD-001), the decisions behind the stack (ADRs), and the
management trail (roadmap/epics/board). This milestone delivers the trail a build can follow — no
application code.

## Delivers (SDDs / epics it groups)

- EPIC-000 — scaffold (cross-cutting; see below). The auth domain bible
  [[../../architecture/sdds/sdd-auth|SDD-001]] was **authored** here as groundwork; the auth
  *capability* is a later milestone.

## Cross-cutting items (no SDD)

- EPIC-000 — scaffold: documentation/architecture foundation + repository init.

## Exit

- [x] Requirements + architecture documented (SRS+SAD, threat model).
- [x] Auth domain SDD authored (SDD-001).
- [x] Stack + testing decisions recorded (ADR-0001, ADR-0002).
- [x] Milestone/epic board stood up.
- [x] Repository initialized and pushed (dev default + main). No version tag — docs only; the first
  tag is `v0.1.0` at the bootstrap release.
