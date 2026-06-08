# ADR-0008 — Deferred production surface: API docs (OpenAPI/Swagger), observability, automated E2E

- **Status:** Accepted
- **Date:** 2026-06-08
- **Milestone / Sprint:** 3 (resource)

## Context

The showcase MVP proves the hybrid-auth path end-to-end (auth issuance + JWKS + local verification +
ownership CRUD). Several surfaces that a **real** deployment of these two services would carry are
intentionally **not built** at this tier. Leaving them undocumented risks two failure modes: a reader
assuming they exist (the README's stack line and SRS+SAD §1.3/§2.3 mention OpenAPI/Swagger
aspirationally), or a contributor adding them ad-hoc without a recorded decision. This ADR records what
is deferred, why, and what stands in for it meanwhile.

## Decision

The following are **deliberately deferred** for the showcase; in a production build of either service
they would be present, and the docs say so explicitly.

1. **API documentation — OpenAPI / Swagger UI (REQ-012).** Neither service wires `springdoc-openapi`
   yet. **Stand-in:** a hand-written API reference in the `README.md` (every route with method, auth,
   request/response shape, success status, and the failure statuses + stable error `code`s). In a real
   deployment both services would expose live Swagger UI (dev profile) generated from the controllers +
   `ProblemDetail` error contract, so the reference can't drift from the code.
2. **Observability.** Only `GET /health` (actuator liveness) is exposed. A production build would add:
   **metrics** (Micrometer → Prometheus; the auth-critical counters — sign-ins, rotations,
   reuse-detections/family-revocations, JWKS fetches/cache-misses on the resource side), **distributed
   tracing** (OpenTelemetry across BFF → auth → resource), **structured JSON logs** with correlation
   ids, and the broader actuator surface (readiness, info) behind auth. Security-relevant events
   (reuse-detection, repeated 401s) would feed alerting.
3. **Automated end-to-end harness.** The cross-service flow (sign-up → sign-in → resource CRUD →
   forged-token 401 → refresh → retry) was validated **manually** for the resource epic (see the
   external integration report, 2026-06-08). A production setup would automate it — e.g. extend the
   `tools/http` collection with a resource section, or a compose-based E2E job in CI gating `dev`. The
   module-level Testcontainers suites already cover each service in isolation; what's deferred is the
   *cross-service, real-network* check.

These are out of scope for the auth and resource MVP epics; they become real work in Phase 2 / a
"productionization" epic.

## Alternatives considered

- **Wire springdoc + Micrometer now** — rejected for the MVP: scope creep beyond proving the auth
  boundary; adds dependencies and surface to maintain before the core is validated. The README
  reference + this ADR cover the gap at zero runtime cost.
- **Say nothing (leave it implicit)** — rejected: the README/SRS already imply Swagger exists; silence
  invites both "where's the Swagger UI?" confusion and ad-hoc additions. Recording the deferral is the
  point.

## Consequences

- **Positive:** expectations are explicit; the README's manual reference gives consumers a real API
  contract today; a future productionization epic has a ready checklist (Swagger, metrics, tracing,
  structured logs, automated E2E).
- **Negative / follow-up:** the README reference is **hand-maintained** — it must be updated alongside
  controller/DTO changes until Swagger replaces it (a Definition-of-Done note for resource routes). No
  runtime metrics/tracing until Phase 2; operating the stack in anger would need them first.

## References

- `../srs+sad.md` §1.3 (NFR — observability), §2.3 (dependencies), §2.6 (future directions),
  `../../open-questions.md` OQ-008, `../../roadmap/03-features/` (resource epic), the README API reference,
  and the external integration report (2026-06-08) whose "no automated E2E harness yet" finding this records.
