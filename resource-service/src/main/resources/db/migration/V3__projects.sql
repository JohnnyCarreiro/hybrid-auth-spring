-- Projects: the top-level owned aggregate of the app domain (SDD-002 §2).
--
-- `owner_id` references the LOCAL users mirror (V2), not auth — keeping the FK inside the `app`
-- database (ADR-0003). ON DELETE CASCADE so removing a (mirrored) user removes their projects.
-- Ownership is the authorization boundary: every operation is scoped to `owner_id = <token sub>`
-- (SDD-002 §4 invariant 1).
CREATE TABLE projects (
  id          uuid PRIMARY KEY,               -- domain-minted UUID v7
  owner_id    uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name        text NOT NULL,
  description text,
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX projects_owner_id_idx ON projects(owner_id);
