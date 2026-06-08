package com.johnnycarreiro.hybridauth.resource.domain.shared;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;

/**
 * Mints time-ordered (UUID v7) identifiers for app aggregates (projects, tasks).
 *
 * <p>Identity for the aggregates this service <em>owns</em> is application-assigned: a {@code
 * Project}/{@code Task} stamps its own id at construction rather than letting the ORM fill it on
 * flush, so the instance is valid in memory before it touches a store (the same rule the
 * auth-service follows — OQ-005). UUID v7's time-ordered layout keeps fresh ids clustered on the
 * B-tree; the JDK has no v7 generator, hence {@code uuid-creator}.
 *
 * <p><b>Not for the user mirror.</b> {@code app.users.id} is the auth user id (the access-token
 * {@code sub}); it is minted by the auth-service and copied in, never generated here.
 */
public final class IdMint {

  private IdMint() {}

  /** A fresh, time-ordered UUID v7. */
  public static UUID next() {
    return UuidCreator.getTimeOrderedEpoch();
  }
}
