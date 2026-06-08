-- F2 JWKS + signing keys (FEAT-006 / SDD-001 §2.1): the RS256 signing-key set.
-- The application assigns `id` (= the JWK `kid`, a UUID v7) and the timestamps; the DB default on
-- `created_at` is a safety net. `expires_at` NULL = the single active key; a non-null value in the
-- future = rotated-out but still servable during grace; in the past = beyond grace (prunable).
-- `private_key` is the JWK JSON encrypted at rest with AES-256-GCM — never stored in clear
-- (SDD-001 §4 invariant 6).
CREATE TABLE jwks (
  id          uuid PRIMARY KEY,
  public_key  text NOT NULL,
  private_key text NOT NULL,
  created_at  timestamptz NOT NULL DEFAULT now(),
  expires_at  timestamptz
);
