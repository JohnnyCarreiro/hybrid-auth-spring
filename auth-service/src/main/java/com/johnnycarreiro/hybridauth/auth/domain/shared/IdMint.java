package com.johnnycarreiro.hybridauth.auth.domain.shared;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;

/**
 * Mints time-ordered (UUID v7) identifiers for auth aggregates.
 *
 * <p>Identity in this domain is <strong>application-assigned</strong>: an aggregate stamps its own
 * id at construction time rather than letting the ORM fill it on flush. UUID v7 is chosen for its
 * time-ordered layout — fresh ids cluster on the B-tree, keeping index locality close to a serial
 * key while staying globally unique and opaque. The JDK has no v7 generator, hence {@code
 * uuid-creator}.
 *
 * <p>This is the single source of new ids for the module; never call {@code UUID.randomUUID()} (v4)
 * for a persisted aggregate — it scatters writes across the index.
 */
public final class IdMint {

  private IdMint() {}

  /** A fresh, time-ordered UUID v7. */
  public static UUID next() {
    return UuidCreator.getTimeOrderedEpoch();
  }
}
