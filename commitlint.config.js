/**
 * Conventional Commits. Enforced in CI against the PR title (which becomes the
 * squash commit on dev/main) — see .github/workflows/ci.yml. Locally, a regex in
 * lefthook's commit-msg hook gives fast feedback without Node.
 */
module.exports = {
  extends: ["@commitlint/config-conventional"],
};
