---
id: FEAT-004
slug: readme
epic: 001-bootstrap
milestone: 001-bootstrap
sdd:
frd:
status: in-progress
depends-on: [001-build-skeleton, 002-ci-pipeline, 003-runtime-baseline]
blocks: []
date: 2026-06-07
---

# FEAT-004 — project README

Realizes [[../02-epics/001-bootstrap|EPIC-001 — bootstrap]] · [[../01-milestones/001-bootstrap|MILESTONE-001]].
Presentation feature — no FRD/SDD. Sources: [[../../architecture/srs+sad|SRS+SAD]] (pitch, API surface),
[[../../architecture/adrs/0002-auth-stack-handbuilt-rs256-issuer|ADR-0002]] (security model),
[[../../architecture/adrs/0003-database-per-service-isolation|ADR-0003]] (two DBs).

## Intent

Turn the scaffolding-era `README.md` into a **portfolio-grade front page**. This is a public showcase
repo: the README is the first thing a technical evaluator reads, so it must land the pitch, show the
architecture (the no-shared-secret JWKS verification is the whole point), and route to the design docs —
while staying honest that the domain flows are still planned. Add an MIT `LICENSE` so the repo states
its terms before the `v0.1.0` cut.

## Acceptance

- [ ] README opens with a one-line pitch, status callout (bootstrap), and badges (CI, Java 21, Spring Boot, MIT).
- [ ] "How it works" section explains the hybrid model and renders an architecture diagram (Mermaid) of
      auth-service issuing session + RS256 JWT, publishing JWKS, and resource-service verifying **locally, no shared secret**.
- [ ] Security model highlights present: RS256 asymmetric signing, Argon2id passwords, refresh
      rotation + reuse-detection, JWKS, database-per-service isolation.
- [ ] Quickstart (`just`/`make`) and the designed API surface are documented; domain routes clearly marked **planned**.
- [ ] Design-docs section links SRS+SAD, ADRs, threat-model, playbook.
- [ ] Capability roadmap shows bootstrap (done) → auth → resource → Phase 2, honestly scoped.
- [ ] `LICENSE` (MIT, © 2026 Johnny Carreiro) committed; README License section points to it.
- [ ] No broken intra-repo links; no claims of unbuilt behavior as done.

## Tasks

- [ ] Rewrite `README.md` to the portfolio front-page structure.
- [ ] Author the Mermaid architecture diagram (GitHub-flavored).
- [ ] Add `LICENSE` (MIT).
- [ ] Cross-check badges (repo path `JohnnyCarreiro/hybrid-auth-spring`, `ci.yml`) and doc links resolve.

## Out of scope

- Architecture diagram as a committed image asset (Mermaid inline is enough at this tier).
- CONTRIBUTING.md / issue templates / social-preview image — defer unless they earn their keep.

## Notes

- Honesty rule: the auth + projects/tasks flows are `planned` in the SRS — the README marks them as
  roadmap, never as shipped. Only bootstrap (skeleton, isolated DBs, runtime baseline, CI) is "done".
- License chosen MIT (2026-06-07) — permissive, signals "study and reuse freely", standard for showcase.

## Retro

_(to fill on close)_
