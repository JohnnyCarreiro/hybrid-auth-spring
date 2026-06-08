package com.johnnycarreiro.hybridauth.resource.services;

import com.johnnycarreiro.hybridauth.resource.infra.database.AppUserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the local {@code app.users} mirror in step with auth identity (ADR-0003 / ADR-0006 /
 * SDD-002 §3 + §8 F-sync) — the resource-service side of the hybrid-auth sync pattern.
 *
 * <p><b>Create-only, opportunistic.</b> {@link #ensureProvisioned} is invoked on every
 * authenticated request (by {@code MirrorSyncInterceptor}, reading the verified token's claims): if
 * no row exists for the {@code sub}, one is inserted from the claims; if it already exists, nothing
 * happens — the mirror is synced on the user's first appearance and never re-synced from the token.
 * So a project can hold a real {@code owner_id} foreign key the moment its owner first calls in,
 * while later changes to email/name on the auth side do <em>not</em> silently overwrite the copy
 * here.
 *
 * <p>Propagating those later changes is deliberately out of scope at this tier: the recommended
 * path is an auth-emitted {@code user.updated} event this service consumes (an outbound callback
 * from auth is the less-favoured alternative — it couples the issuer to its consumers). Both are
 * deferred (ADR-0006). The cost is bounded eventual staleness on non-identity fields; ownership
 * (keyed by the immutable {@code sub}) is never affected.
 */
@Service
public class UserMirror {

  private final AppUserRepository users;

  public UserMirror(AppUserRepository users) {
    this.users = users;
  }

  /**
   * Provision the mirror row for {@code id} from the token claims if it is not already present
   * (create-only). Idempotent and concurrency-safe — backed by {@code INSERT … ON CONFLICT DO
   * NOTHING}.
   */
  @Transactional
  public void ensureProvisioned(UUID id, String email, boolean emailVerified) {
    users.provisionIfAbsent(id, email, emailVerified);
  }
}
