# Software Design Documents (SDDs)

SDDs document the **tactical design** of a coherent **domain** (one bounded context, or for non-DDD projects a major area). They are companions to the SAD: SAD is strategic (which contexts exist, how they communicate); SDD is tactical (inside one context, what types and operations).

**An SDD is a domain, not an epic.** It is a **management-agnostic** doc: it carries **no** `epic:`/`milestone:` field and never points at management constructs. A management **Epic references this SDD** via its `sdd:` field (Epic : SDD ≈ 1 : 1); a cross-cutting `refactor`/`infra` epic references none. The reference is one-way — the epic points at the SDD; the SDD never points back. See `playbook-base.md` §22.1.

The SDD frontmatter sets `subdomain-type: core | supporting | generic`:

- **core** — the differentiating heart of the product.
- **supporting** — specific to the business but not the competitive edge.
- **generic** — off-the-shelf-shaped (e.g. `auth`).

**FRD absorbed at this tier.** At `small`, the FRD layer (the "what" of each functionality) lives **inside the SDD** as §8 "Functionalities" — one block per functionality with business intent + acceptance criteria + fine validation rules + its Tasks. The build is still **feature-keyed** (one Feature per functionality, 1:1). FRDs split out into their own flat files `../frds/frd-<slug>.md` only at `medium`+.

## When to write an SDD

- A bounded context has more than ~3 aggregates / domain types.
- A non-trivial public API surface needs invariants documented.
- Multiple ADRs accumulate around one area; an SDD consolidates them.

For prototypes, design decisions live in `srs+sad.md` (§3) + ADRs. SDDs start at the `small` tier when they help. Promotion triggers in `playbook-base.md` §22.

## Authoring an SDD — RPA is a mental discipline, one flat file

The SDD is a **flat file** `sdds/sdd-<slug>.md` — the single artifact. RPA (Research → Plan → Act) is the **mental** discipline for authoring it well: Research (pull the SAD, related ADRs, SRS) → Plan (outline aggregates / entities / behaviors / the endpoint table) → Act (write the doc). There are **no** `research.md`/`plan.md` siblings and **no** folder by default. Durable decisions surfaced while authoring go to an **ADR** — that is the only part of the "research" that persists.

> **Rare-audit exception:** when it genuinely matters to record *what* the AI evaluated and *how* (audit / sensitive handoff), promote that one node to a folder and keep a single `research.md` (the plan folded inside) next to the doc. This case is **RARE but exists; default = no research file.**

## Naming

`sdds/sdd-<slug>.md`. Example: `sdds/sdd-billing.md`, `sdds/sdd-event-bus.md`.

## Template

Use the Templater snippet `.obsidian/templates/sdd-template.md`. Sections:

1. Domain / context
2. Aggregates / domain types
3. Use cases / operations
4. Invariants
5. Errors
6. Ports / external dependencies
7. Behavioral API surface (routes expose use-case/intent, not status patches — playbook §6.1)
8. Functionalities (absorbed FRDs — at `small`)
9. Open items

## Example index

| ID | Domain | subdomain-type | Status |
|----|--------|----------------|--------|
| SDD-001 | <bounded context> | core | draft |
