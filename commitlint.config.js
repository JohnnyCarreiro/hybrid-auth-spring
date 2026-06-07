/**
 * Conventional Commits. Enforced in CI against the PR title (which becomes the
 * squash commit on dev/main) — see .github/workflows/ci.yml. Locally, a regex in
 * the .githooks/commit-msg POSIX hook gives fast feedback without Node.
 */
module.exports = {
  extends: ["@commitlint/config-conventional"],
};
