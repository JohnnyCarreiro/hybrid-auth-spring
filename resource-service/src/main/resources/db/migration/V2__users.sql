-- The identity MIRROR for the app database (ADR-0003 / ADR-0006 / SDD-002 §2).
--
-- A create-only cache of a subset of auth identity, keyed by the auth user id (the access-token
-- `sub` claim). `auth.users` is the source of truth; this is a local copy so the app domain can own
-- a `projects.owner_id` foreign key without reaching across the boundary.
--
-- There is NO cross-database foreign key to auth.users — the link is logical, by value only
-- (ADR-0003). The id is the UUID v7 minted by the auth-service; it is never minted here.
-- Rows are provisioned opportunistically on the first authenticated request (JIT) and, at this tier,
-- never updated from the token afterwards (ADR-0006: create-only; the update path is deferred to an
-- auth-side event the resource-service would consume).
CREATE TABLE users (
  id             uuid PRIMARY KEY,            -- = auth user id (JWT `sub`); minted by auth, mirrored here
  email          text NOT NULL,
  email_verified boolean NOT NULL DEFAULT false,
  name           text,                        -- not carried by the access token; populated by a future sync
  created_at     timestamptz NOT NULL DEFAULT now(),
  updated_at     timestamptz NOT NULL DEFAULT now()
);
