package com.johnnycarreiro.hybridauth.resource.infra.database;

import com.johnnycarreiro.hybridauth.resource.domain.identity.AppUser;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence port for the local identity {@link AppUser} mirror (SDD-002 §6). */
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

  /**
   * Provision a mirror row for a freshly-seen auth subject, <strong>create-only</strong>
   * (ADR-0006).
   *
   * <p>{@code INSERT … ON CONFLICT (id) DO NOTHING} makes this idempotent and concurrency-safe (two
   * simultaneous first-requests for the same {@code sub} produce one row, no lost-update, no thrown
   * unique violation) and — crucially — it <em>never updates</em> an existing row: the mirror is
   * synced on creation only, so a later token carrying a changed email does not overwrite the
   * stored copy (that propagation is deferred to an auth-side event — ADR-0006 / SDD-002 §8
   * F-sync). Returns the number of rows inserted (0 when the row already existed).
   */
  @Modifying
  @Query(
      value =
          "INSERT INTO users (id, email, email_verified, created_at, updated_at) "
              + "VALUES (:id, :email, :emailVerified, now(), now()) "
              + "ON CONFLICT (id) DO NOTHING",
      nativeQuery = true)
  int provisionIfAbsent(
      @Param("id") UUID id,
      @Param("email") String email,
      @Param("emailVerified") boolean emailVerified);
}
