package com.johnnycarreiro.hybridauth.auth.sessions;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence port for the {@link Session} aggregate.
 *
 * <p>Beyond {@code save} (open a root, F3), refresh rotation (F5) needs two store operations that
 * are load-bearing for reuse-detection:
 *
 * <ul>
 *   <li>{@link #findByTokenHashForUpdate} — the pessimistically-locked lookup of the presented
 *       session. Run inside the rotation {@code @Transactional}, the {@code PESSIMISTIC_WRITE} lock
 *       emits {@code SELECT … FOR UPDATE}, so two concurrent rotations of the <em>same</em> token
 *       serialize: the second blocks until the first commits, then sees {@code rotatedAt} set and
 *       is treated as reuse (SDD-001 §4 invariants 2–3).
 *   <li>{@link #revokeFamily} — the one-shot family kill: a single bulk {@code UPDATE} that revokes
 *       every still-active session sharing a {@code familyId} (invariant 2).
 * </ul>
 */
public interface SessionRepository extends JpaRepository<Session, UUID> {

  /**
   * Find the session whose {@code tokenHash} matches, taking a write lock on the row ({@code SELECT
   * … FOR UPDATE}). MUST be called inside the rotation transaction so the lock is held until commit
   * — that serialization is the whole concurrency defense (SDD-001 §6 / §4 invariant 3).
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from Session s where s.tokenHash = :hash")
  Optional<Session> findByTokenHashForUpdate(@Param("hash") String hash);

  /**
   * Revoke every still-active session in a family in one statement (SDD-001 §4 invariant 2): {@code
   * UPDATE sessions SET revoked_at = :now WHERE family_id = :familyId AND revoked_at IS NULL}.
   * Returns the number of rows touched (for the security-event log / tests).
   */
  @Modifying
  @Query(
      "update Session s set s.revokedAt = :now"
          + " where s.familyId = :familyId and s.revokedAt is null")
  int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);
}
