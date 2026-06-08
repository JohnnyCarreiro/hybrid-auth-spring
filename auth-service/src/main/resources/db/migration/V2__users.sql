-- F1 sign-up (FEAT-005 / SDD-001 §2.1): the identity table.
-- The application assigns `id` (UUID v7) and the timestamps; the DB defaults are a safety net.
CREATE TABLE users (
  id             uuid PRIMARY KEY,
  email          text NOT NULL UNIQUE,
  password_hash  text NOT NULL,
  email_verified boolean NOT NULL DEFAULT false,
  name           text,
  created_at     timestamptz NOT NULL DEFAULT now(),
  updated_at     timestamptz NOT NULL DEFAULT now()
);
