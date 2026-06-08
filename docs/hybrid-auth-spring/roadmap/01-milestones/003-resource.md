---
id: MILESTONE-003
slug: resource
title: Resource
window: 2026-06
status: done
date: 2026-06-08
---

# MILESTONE-003 — Resource

1:1 with EPIC-003.

## Goal

The resource-service consumes the hybrid credential end-to-end: it verifies the access JWT locally
against the auth-service JWKS (no shared secret, refetch-on-rotation), mirrors auth identity locally
(create-only), and serves ownership-scoped CRUD over projects and tasks — closing the
sign-in → protected-resource path the system exists to demonstrate. It issues and rotates **no**
tokens (that is the BFF↔auth concern). Covered by Testcontainers integration tests + a direct verifier
unit test against an in-JVM JWKS stub.

## Delivers (SDDs / epics it groups)

- SDD-002 — resource (the tactical bible).
- EPIC-003 — resource.

## Exit

- [x] SDD-002 §8 F1–F4 acceptance met (verifier · mirror · projects CRUD · tasks CRUD).
- [x] Resource-critical flows covered by integration tests (Testcontainers, ADR-0001).
- [x] Released: `dev → main` tagged `v0.3.0` (playbook §16.4 / §5).
