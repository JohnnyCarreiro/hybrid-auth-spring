-- Tasks: child of a project (SDD-002 §2). Ownership is DERIVED through the parent project — a task
-- carries no `owner_id` of its own; authorization loads the task, then checks its project's owner
-- (SDD-002 §4 invariant 2). ON DELETE CASCADE so deleting a project removes its tasks.
--
-- `status` is a free `text` column constrained at the application layer to the TaskStatus enum
-- (TODO | DOING | DONE), mapped @Enumerated(STRING) — the DDL reads plainly and a new status is an
-- enum change, not a migration.
CREATE TABLE tasks (
  id          uuid PRIMARY KEY,               -- domain-minted UUID v7
  project_id  uuid NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  title       text NOT NULL,
  description text,
  status      text NOT NULL DEFAULT 'TODO',
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX tasks_project_id_idx ON tasks(project_id);
