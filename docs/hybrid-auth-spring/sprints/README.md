# Sprints

Time-boxed execution units. A sprint contains the features being worked on in that window.

This directory is the home of **Feature Placement Mode A — sprint-bound** (playbook §23). If your project runs kanban-flow without time-boxed sprints (typical for 1–2 contributors), use Mode B (features under `../epics/<id>-<slug>/features/`) or Mode C (features under `../features/` at docs root) instead. The Templater snippet `feature-template.md` asks for the FRD build dir at feature creation.

At `small` tier the SDD is a flat file `../architecture/sdds/sdd-<slug>.md` and the **FRD is absorbed into the SDD** (§8 "Functionalities"), so the build is **feature-keyed** (`features/<feature-slug>/`, one Feature = one functionality, 1:1). FRDs split out into their own flat files `../architecture/frds/frd-<slug>.md` only at **`medium`**+ — the build stays feature-keyed (there is **never** a `frds/` folder in the build).

## Layout

```
sprints/
├── sprint-01/
│   ├── README.md                        # sprint goal + scope + planning section + retrospective
│   └── features/
│       ├── <feature-slug>/README.md     # live trail: goal, acceptance + Tasks (from the SDD's §8); the CODE is the Act
│       └── <other-feature-slug>/README.md
└── sprint-02/
    └── ...
```

## RPA is a mental discipline, not files

RPA (Research → Plan → Act) is the discipline for authoring **every node** well — it produces exactly one artifact per node, never a fixed file set:

- The **SDD** is the **Act** of an authoring pass: Research → Plan → Act = the flat `sdd-<slug>.md` bible (see `../architecture/sdds/`). No `research.md`/`plan.md` siblings; durable decisions go to an **ADR**. At `small` the FRD lives inside the SDD's §8.
- At **build** time the **code is the Act**; each feature `README.md` is the **live tracker** — the SDD §8 block's Tasks pulled in as `- [ ]` checklists, ticked as work happens, surprises and punted items captured inline, with a short closing retro that feeds the sprint retrospective.
- There is **no `research.md`/`plan.md`/`act.md`** — the README + code carry the trail. (Rare-audit exception: one `research.md` next to a node only for audit/sensitive handoff — RARE.)

## How a sprint starts

1. Pick cards from `../roadmap/_dashboard/board.md` that are in **Initial** and that fit the sprint window.
2. Move them to **In Progress** in the roadmap board.
3. Create `sprint-NN/README.md` with goal + scope + the chosen cards + a short **Planning** section (capacity, picks; at `small` planning is a section here, not a separate `planning.md`).
4. For each card, create `features/<feature-slug>/README.md` (use the Templater snippet `feature-template.md`; feature build dir = `sprints/sprint-NN` → it lands the feature under `features/`).
5. Build each feature bottom-up; the feature `README.md` tracks Tasks; the code is the Act.

## How a sprint ends

1. For each completed feature, move its roadmap card to **Done** and add a one-line entry under "Done in sprint-NN" in the sprint README.
2. Write a brief retrospective at the bottom of `sprint-NN/README.md`: what went well, what didn't, what to change.
3. Items that didn't ship: either return to **Initial** in the roadmap (re-plan next sprint) or back to the **Backlog** (de-commit).

## Sprint README template

```markdown
---
sprint: NN
window: YYYY-MM-DD .. YYYY-MM-DD
status: planned | active | closed
---

# Sprint NN

## Goal

<one paragraph>

## Planning

<capacity, justified picks, what was not picked and why>

## Cards in scope

- [ ] [[../../roadmap/<id>-<slug>|<id> — <slug>]]
- [ ] ...

## Done in this sprint

- ...

## Retrospective

**What went well:**

**What did not:**

**Changes for next sprint:**
```
