# Epics

Multi-sprint groupings of work. An epic spans more than one sprint and has its own completion criteria (`exits_with`). Distinct from `../roadmap/` (single-card commitments) and `../backlogs/` (ideas).

In the management chain **Milestone → Epic → Feature → Task**, the Epic sits below a roadmap **Milestone** (a delivery grouping; see `../roadmap/`) and above the Features that realize it (one Feature = one FRD, 1:1).

## Layout

```
epics/
├── _dashboard/
│   └── board.md          ← Obsidian Kanban; columns: Planned / Active / Done / Parked
├── <id>-<slug>.md        ← per-epic file (one flat file per epic at this tier)
└── README.md             ← this file
```

## Types

Each epic declares a `type` in its frontmatter:

- **capability** — themed body of product work (`authentication`, `billing`). Most product work is `capability`.
- **refactor** — quality / restructuring.
- **infra** — CI/CD, observability, dev tools. (The scaffolding epic `EPIC-001 — bootstrap` is `type: infra`.)

Temporal grouping — `bootstrap`, `MVP`, `v1` — is **not** an epic type: it's a **Milestone** (a roadmap-level delivery grouping; see `../roadmap/`). A Milestone spans 1..N epics/SDDs (Milestone : SDD = 1 : N).

### Epic references an SDD (it does not equal one)

An **Epic references one SDD** via its `sdd:` field — it is *not* the same object as the SDD (Epic : SDD ≈ 1 : 1). A **domain** epic (`capability`) references exactly one bounded context — its agnostic tactical bible, the flat file `../architecture/sdds/sdd-<slug>.md`. Set the epic's `sdd:` field to that SDD's ID. A **cross-cutting** epic (`refactor`, `infra`) has no bounded context — leave `sdd:` blank. The reference is one-way: the epic points at the SDD; the SDD (agnostic, no `epic:` field) never points back (playbook §22.1).

## Workflow

1. **Plan** — Create the epic file via the Templater snippet `.obsidian/templates/epic-template.md`. Fill in `why`, `outcome`, `scope`, `exits_with`. Add to the kanban under **Planned**.
2. **Activate** — When a sprint commits to advancing this epic, move it to **Active** and set the sprint's `active_epic:` to point here.
3. **Track** — Roadmap cards belonging to this epic should carry `epic: <id>-<slug>` in their frontmatter. Sprint READMEs list `active_epic:`.
4. **Close** — When all `exits_with` are checked, move to **Done**. Per playbook §16.4, an epic closing triggers a `dev → main` release (semver bump, tag).
5. **Park** — Use **Parked** for epics deferred indefinitely. Document why in the epic file.

## Relationship to other artefacts

```
Backlog (ideas)
    ↓ promote
Roadmap (commitments)         epic: <id>-<slug>   ← roadmap cards reference an epic
    ↓ select for sprint
Sprint (execution)            active_epic: <slug> ← sprint serves one (or few) epic(s)
    ↓ Feature (1:1 FRD) → Tasks (1–2 day units)
    ↓ exits_with all checked
Epic → Done → release (playbook §16.4)
```

## Hosting features inside an epic (Mode B)

When the project runs **Feature Placement Mode B — epic-bound** (playbook §23), features live under the epic rather than under sprints:

```
epics/
└── <id>-<slug>/                          ← the epic (references one SDD via `sdd:`)
    ├── README.md                         ← the epic; frontmatter: sdd: sdd-<slug>
    └── features/<feature-slug>/README.md ← build, feature-keyed (1 Feature = 1 FRD, 1:1)
                                           ←   live trail: goal, acceptance (from the SDD's §8) + Tasks
                                           ←   the README is the trail; the CODE is the Act
```

The build is **feature-keyed** (1 Feature = 1 FRD, 1:1). At `small` the FRD is **absorbed in the SDD's §8 "Functionalities"** — there is no separate FRD file — so the feature README pulls its acceptance + Tasks from that SDD block. There is **no `frds/` folder in the build** and **no `research.md`/`plan.md`/`act.md`**: RPA is a mental discipline, the code is the Act, durable decisions go to an ADR.

For Mode B, convert the epic file (`<id>-<slug>.md`) into a folder (`<id>-<slug>/README.md`) before adding features. The kanban entry still points at the folder via `[[<id>-<slug>/README|<id> — <name>]]`.

## Card file template

See the Templater snippet at `.obsidian/templates/epic-template.md` (run via Templater command palette). It generates a file with id, slug, type, owner, exits_with, and progress log.
