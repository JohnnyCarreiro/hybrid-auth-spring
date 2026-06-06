---
id: FEAT-000
slug: docs-foundation
epic: 000-scaffold
milestone: 000-scaffold
sdd:
frd:
status: done
depends-on: []
blocks: []
date: 2026-06-06
---

# FEAT-000 — docs foundation

Realizes [[../02-epics/000-scaffold|EPIC-000 — scaffold]] · [[../01-milestones/000-scaffold|MILESTONE-000]].
Infra feature — no FRD/SDD; the deliverable **is** the documentation/architecture foundation.

## Intent

Stand up the project's documentation and architecture foundation before any code, so the build has a
trail to follow.

## Acceptance

- [x] SRS+SAD + threat model in `architecture/`.
- [x] Auth domain SDD (SDD-001) authored.
- [x] ADR-0001 (testing) + ADR-0002 (auth issuer stack) recorded.
- [x] Methodology + playbook + management boards in place.
- [x] Repository initialized (dev default + main) and pushed.

## Tasks

- [x] Scaffold the Obsidian docs vault + `.gitignore`.
- [x] Write SRS+SAD, threat model, open questions.
- [x] Author SDD-001 and the two ADRs.
- [x] Stand up milestones / epics / features boards.
- [x] Init repo, commit hygiene, first push.

## Retro

**Shipped:** docs/architecture foundation + repo. **Carry forward:** EPIC-001 bootstrap turns this
into a running skeleton.
