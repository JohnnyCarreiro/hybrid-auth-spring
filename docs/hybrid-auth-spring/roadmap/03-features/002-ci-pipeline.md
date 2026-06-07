---
id: FEAT-002
slug: ci-pipeline
epic: 001-bootstrap
milestone: 001-bootstrap
sdd:
frd:
status: done
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

- [x] CI (GitHub Actions) on PR→`dev` and PR→`main`: `./gradlew build` + tests **green** (Docker available for Testcontainers — ADR-0001).
- [x] Spotless gate (`spotlessCheck`, google-java-format — OQ-003).
- [x] commitlint gate over the **PR title** (`commitlint.config.js`).
- [x] **lefthook** wired locally via `./.tools/lefthook` + `.githooks/` (no global/npm): pre-commit (Spotless on staged + block commits on dev/main), commit-msg (Conventional regex), pre-push (block pushes to dev/main).
- [x] **Branch protection** active: `dev` (require PR), `main` (require PR + no-bypass + no direct/force push).
- [x] README: `hooks-install` note + contributor flow.

## Tasks

- [x] `.github/workflows/ci.yml`: Gradle build + test (Docker for Testcontainers) + `spotlessCheck`.
- [x] commitlint job (Node in the runner) over the PR title + `commitlint.config.js`.
- [x] Spotless plugin wired in Gradle (google-java-format).
- [x] `lefthook.yml` + `.githooks/{pre-commit,commit-msg,pre-push}` wrappers + `just/make hooks-install` (download binary → `.tools/`, set `core.hooksPath`); gitignore `.tools/`.
- [x] Branch protection on `dev` + `main` via `gh api` (main: `enforce_admins`) — documented.
- [x] `docker-buildx` in CI for the build cache (ADR-0004 tip).
- [x] Author `playbook-java.md` (OQ-001) capturing the wired conventions.
- [x] Note the release-flow change: `dev→main` via PR now (main protected).

## Notes

- CI is wired but its first green run is exercised on the **epic→dev PR** (CI only runs on PRs).
- Branch protection applied on `dev`/`main` (PR-only; `main` no-bypass) — the `dev→main` release is now a PR; local commits in CI contexts use `--no-verify` (the human dev keeps the hooks).

- Correction: local hooks are **plain POSIX `.githooks/`** (not lefthook) — see playbook-java §6; lefthook needs a global/npm/PATH binary and auto-syncs over custom wrappers.

## Release automation (decided 2026-06-07 — wire with the v0.1.0 release)

- **release-please** (GitHub Action) cuts releases from Conventional Commits: it maintains a "release PR"
  that bumps the version + updates `CHANGELOG.md`; merging that PR creates the git tag + GitHub Release.
  No Node in the repo (it's an Action).
- **Wired 2026-06-07** — watches `main` (the `dev → main` release merge):
  - `.github/workflows/release-please.yml` — `googleapis/release-please-action@v4`, `push` on `main`,
    `contents: write` + `pull-requests: write`.
  - `release-please-config.json` — `release-type: simple`, `bump-minor-pre-major: true`,
    `bump-patch-for-minor-pre-major: false`; `extra-files` generic updater on `build.gradle.kts`.
  - `.release-please-manifest.json` — seeded root package `.` at `0.0.0` so the first `feat:` cut is **v0.1.0**.
  - `build.gradle.kts` version line annotated `// x-release-please-version` (now `0.0.0`, no `-SNAPSHOT`;
    the release PR rewrites it). `simple` won't create a stray `version.txt` (`createIfMissing: false`).
- **Resolved 2026-06-07 (PAT):** the release PR is opened by the default `GITHUB_TOKEN`, which does **not**
  trigger the `build`/`commit-convention` checks that `main`'s branch protection requires (GitHub
  anti-recursion) — so the PR would never become mergeable. Fixed by opening it under a **fine-grained PAT**
  (`token: ${{ secrets.RELEASE_PLEASE_TOKEN }}`, Contents RW + Pull requests RW on this repo), so the
  checks run. **Follow-up:** migrate to a GitHub App token (no PAT expiry) — tracked in `open-questions.md`.
