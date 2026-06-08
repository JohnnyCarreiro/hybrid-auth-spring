# Architecture Decision Records (ADRs)

ADRs capture decisions that have lasting structural consequences. Short, dated, and linked from the playbook / SAD when relevant.

## Status legend

- **Draft** — title and context noted; decision not yet made or not yet written down.
- **Proposed** — content drafted, awaiting review.
- **Accepted** — decision is in force; implementation reflects it.
- **Superseded** — replaced by a later ADR (link to successor).

## Index

| # | Title | Status | Milestone / Sprint |
|---|-------|--------|----------------|
| [0001](0001-testing-stack-junit-mockito.md) | Testing stack: JUnit 5 + Mockito (+ Testcontainers for integration) | Accepted | 0 |
| [0002](0002-auth-stack-handbuilt-rs256-issuer.md) | Auth stack: hand-built RS256 issuer on Spring Security 6 | Accepted | 2 |
| [0003](0003-database-per-service-isolation.md) | Database-per-service: isolate auth and app data stores | Accepted | 1 |
| [0004](0004-runtime-baseline-jetty-flyway.md) | Runtime baseline: Jetty embedded server + Flyway migrations | Accepted | 1 |
| [0005](0005-resource-server-jwks-verifier.md) | Resource-server token verification: a hand-built JWKS verifier | Accepted | 3 |
| [0006](0006-user-identity-sync.md) | User identity sync: create-only mirror, deferred event-based updates | Accepted | 3 |
| [0007](0007-identity-mirror-sync-events.md) | Identity-mirror sync: provision-before-authz + event-driven updates | Accepted | 3 |
| [0008](0008-deferred-operational-surface.md) | Deferred production surface: OpenAPI/Swagger, observability, automated E2E | Accepted | 3 |

## Template

```markdown
# ADR-NNNN — Title

- **Status:** Draft | Proposed | Accepted | Superseded by ADR-XXXX
- **Date:** YYYY-MM-DD
- **Milestone / Sprint:** N

## Context

What problem are we solving? What constraints exist?

## Decision

State as a sentence: "We will use X."

## Alternatives considered

- (a) ... — rejected because ...
- (b) ... — rejected because ...

## Consequences

Positive and negative. What becomes easier? What becomes harder? What needs follow-up?

## References

Links to issues, prior art, related ADRs.
```

The Templater plugin (in `.obsidian/templates/adr-template.md`) auto-generates this skeleton with prompts for ID, title, and milestone.

## When to write a new ADR

See `../playbook/playbook-base.md` § "ADRs". In short: changing a default that affects security posture, adding a new external dep, changing a public surface, or picking between approaches with lasting trade-offs.
