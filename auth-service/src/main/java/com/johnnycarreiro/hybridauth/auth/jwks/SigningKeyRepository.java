package com.johnnycarreiro.hybridauth.auth.jwks;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence port for the {@link SigningKey} aggregate. */
public interface SigningKeyRepository extends JpaRepository<SigningKey, UUID> {

  /** The single active key — the row whose {@code expires_at} is NULL — if one exists. */
  Optional<SigningKey> findFirstByExpiresAtIsNull();

  /**
   * The servable set: the active key ({@code expires_at} NULL) plus every retired key still inside
   * its grace window ({@code expires_at} strictly after {@code now}). These are the keys published
   * by {@code /.well-known/jwks.json}.
   */
  List<SigningKey> findByExpiresAtIsNullOrExpiresAtAfter(Instant now);
}
