---
id: FEAT-006
slug: auth-jwks
epic: 002-auth
milestone: 002-auth
sdd: SDD-001
frd: F2
status: planned
depends-on: []
blocks: [007-auth-signin, 008-auth-me]
date: 2026-06-07
---

# FEAT-006 — JWKS + signing keys

Realizes [[../02-epics/002-auth|EPIC-002 — auth]] · [[../01-milestones/002-auth|MILESTONE-002]].
Build of **[[../../architecture/sdds/sdd-auth|SDD-001]] §8 F2** — JWKS + signing keys (the issuance
foundation). Acceptance + tasks are the SDD's.

## Intent

Stand up the RS256 key set with mint/verify capability and publish the public keys via
`/.well-known/jwks.json`; lazy rotation (90 d + 30 d grace); private key encrypted at rest, never served.

## Sequence

- **Depends on:** — (parallelizable with F1; both are foundations).
- **Blocks:** F3 sign-in (needs a key to mint), F4 `/me` (needs keys to verify).
- **Why it moved up** (vs the SDD's old reading order): minting an access JWT in sign-in requires a
  `JWKSource` + key, so the key foundation must land before F3 — see SDD §8 ordering note.

## Done when

SDD-001 §8 F2 acceptance met (JWKS serves active + grace keys, `max-age=600`; rotation keeps grace-window
verification; private key encrypted, never served) + feature DoD (tests, CI green, PR into `epic/002-auth`).
