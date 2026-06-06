---
id: FEAT-002
slug: ci-pipeline
epic: 001-bootstrap
milestone: 001-bootstrap
sdd:
frd:
status: planned
depends-on: [001-build-skeleton]
blocks: []
date: 2026-06-06
---

# FEAT-002 — ci pipeline

Realizes [[../epics/001-bootstrap|EPIC-001 — bootstrap]] · [[../milestones/001-bootstrap|MILESTONE-001]].
Infra feature — no FRD/SDD. Resolves OQ-002 / OQ-003.

## Intent

CI that gates every PR (build + test) and a lint/format gate, so `dev` stays green. Release semantics
from Conventional Commits (playbook §16.2).

## Acceptance

- [ ] Workflow runs `./gradlew build` + tests on PR→`dev` and push→`main`.
- [ ] Docker available in CI for Testcontainers (ADR-0001).
- [ ] Lint/format gate runs (Spotless + google-java-format — OQ-003).
- [ ] `commitlint` gate enforces Conventional Commits.

## Tasks

- [ ] `.github/workflows/` Gradle CI (build + test, Docker service).
- [ ] Spotless wiring + `spotlessCheck` in the gate.
- [ ] commitlint config + gate.
- [ ] Author `playbook-java.md` (OQ-001) capturing the wired conventions.
