---
id: MILESTONE-002
slug: auth
title: Auth
window: 2026-06
status: active
date: 2026-06-07
---

# MILESTONE-002 — Auth

1:1 with EPIC-002.

## Goal

The auth-service issues and rotates hybrid credentials and publishes JWKS: sign-up, signing keys, sign-in,
`/me`, refresh rotation + reuse-detection (the centerpiece), and sign-out — all working end-to-end against
the isolated `auth` database, covered by Testcontainers integration tests. The private key never leaves the
auth-service.

## Delivers (SDDs / epics it groups)

- SDD-001 — auth (the tactical bible).
- EPIC-002 — auth.

## Exit

- [ ] SDD-001 §8 F1–F6 acceptance met.
- [ ] Auth-critical flows covered by integration tests (Testcontainers, ADR-0001).
- [ ] Released: `dev → main` tagged `v0.2.0` (playbook §16.4 / §5).
