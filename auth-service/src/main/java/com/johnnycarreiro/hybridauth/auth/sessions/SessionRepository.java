package com.johnnycarreiro.hybridauth.auth.sessions;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence port for the {@link Session} aggregate.
 *
 * <p>F3 only needs {@code save} (open a root session). The pessimistic-locked finder that rotation
 * needs ({@code SELECT … FOR UPDATE} on the presented token's session, SDD-001 §6) is intentionally
 * <em>not</em> added here — F5 will introduce a {@code @Lock(PESSIMISTIC_WRITE)} {@code
 * findByTokenHash} when it owns the atomic-rotation rule.
 */
public interface SessionRepository extends JpaRepository<Session, UUID> {}
