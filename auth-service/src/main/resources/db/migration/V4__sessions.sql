-- F3 sign-in (FEAT-007 / SDD-001 §2.1): the refresh-session table.
-- A `sessions` row is one node in a refresh-token rotation chain. The application assigns `id`
-- (UUID v7) and `family_id` (a fresh UUID v7 on a root login, inherited on rotation — invariant 1,
-- so the rule survives a store swap); `family_id` therefore has NO DB default. The timestamps' DB
-- defaults are a safety net. `token_hash` is the SHA-256 of the opaque refresh token (the raw token
-- is returned to the client once and never stored — invariant 7); its UNIQUE constraint also makes a
-- replayed hash a hard conflict. The rotation columns (`parent_id`, `rotated_at`, `revoked_at`) are
-- created now but only exercised from F5 (rotation) / F6 (sign-out); a root login leaves them NULL.
-- `parent_id` self-references and is nulled if a parent row is deleted; `user_id` cascades on user
-- deletion.
CREATE TABLE sessions (
  id          uuid PRIMARY KEY,
  user_id     uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash  text NOT NULL UNIQUE,                 -- SHA-256 of the opaque refresh token
  expires_at  timestamptz NOT NULL,
  family_id   uuid NOT NULL,                         -- groups all rotation descendants of one login
  parent_id   uuid REFERENCES sessions(id) ON DELETE SET NULL,
  rotated_at  timestamptz,
  revoked_at  timestamptz,
  ip_address  text,
  user_agent  text,
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX sessions_user_id_idx       ON sessions(user_id);
CREATE INDEX sessions_family_id_idx     ON sessions(family_id);
CREATE INDEX sessions_family_active_idx ON sessions(family_id, rotated_at);
