---
id: FEAT-002
slug: ci-pipeline
epic: 001-bootstrap
milestone: 001-bootstrap
sdd:
frd:
status: in-progress
depends-on: [001-build-skeleton]
blocks: []
date: 2026-06-06
---

# FEAT-002 — ci pipeline

Realizes [[../02-epics/001-bootstrap|EPIC-001 — bootstrap]] · [[../01-milestones/001-bootstrap|MILESTONE-001]].
Infra feature — no FRD/SDD. Resolves OQ-002 / OQ-003.

## Intent

CI that gates every PR (build + test) and a lint/format gate, so `dev` stays green. Release semantics
from Conventional Commits (playbook §16.2).

## Decisions (locked 2026-06-07)

- **commitlint:** **CI-only** (Node stays in the runner, never in the repo); `commitlint.config.js`
  (extends `@commitlint/config-conventional`); validated against the **PR title** — which becomes the
  squash commit on `dev`/`main`, so individual (squashed) feat commits aren't linted.
- **Local hooks:** **lefthook**, **local binary** (no global install, no npm) — `just hooks-install`
  downloads it to `./.tools/lefthook` (gitignored) and points `core.hooksPath` at committed `.githooks/`
  wrappers that call `./.tools/lefthook run <hook>`. README documents the step.
- **Branch protection = the real "no skip":** local hooks are always bypassable (`--no-verify`), so the
  authoritative guard is **server-side GitHub branch protection** — `dev` requires PR (no direct push);
  `main` requires PR + **no-bypass (enforce_admins)** + no direct/force push. ⇒ the `dev→main` release
  becomes a PR.
- **CI build cache:** install `docker-buildx` in CI to use the BuildKit cache-mount (ADR-0004 tip); local
  builds stay buildx-free.

## Acceptance

- [ ] CI (GitHub Actions) on PR→`dev` and PR→`main`: `./gradlew build` + tests **green** (Docker available for Testcontainers — ADR-0001).
- [ ] Spotless gate (`spotlessCheck`, google-java-format — OQ-003).
- [ ] commitlint gate over the **PR title** (`commitlint.config.js`).
- [ ] **lefthook** wired locally via `./.tools/lefthook` + `.githooks/` (no global/npm): pre-commit (Spotless on staged + block commits on dev/main), commit-msg (Conventional regex), pre-push (block pushes to dev/main).
- [ ] **Branch protection** active: `dev` (require PR), `main` (require PR + no-bypass + no direct/force push).
- [ ] README: `hooks-install` note + contributor flow.

## Tasks

- [ ] `.github/workflows/ci.yml`: Gradle build + test (Docker for Testcontainers) + `spotlessCheck`.
- [ ] commitlint job (Node in the runner) over the PR title + `commitlint.config.js`.
- [ ] Spotless plugin wired in Gradle (google-java-format).
- [ ] `lefthook.yml` + `.githooks/{pre-commit,commit-msg,pre-push}` wrappers + `just/make hooks-install` (download binary → `.tools/`, set `core.hooksPath`); gitignore `.tools/`.
- [ ] Branch protection on `dev` + `main` via `gh api` (main: `enforce_admins`) — documented.
- [ ] `docker-buildx` in CI for the build cache (ADR-0004 tip).
- [ ] Author `playbook-java.md` (OQ-001) capturing the wired conventions.
- [ ] Note the release-flow change: `dev→main` via PR now (main protected).
