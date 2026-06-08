package com.johnnycarreiro.hybridauth.resource.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.johnnycarreiro.hybridauth.resource.domain.identity.AppUser;
import com.johnnycarreiro.hybridauth.resource.infra.database.AppUserRepository;
import com.johnnycarreiro.hybridauth.resource.support.AbstractResourceIT;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The hybrid-auth sync pattern (ADR-0006): the {@code app.users} mirror is provisioned on first
 * sight (JIT) and is <strong>create-only</strong> — a later call with changed identity claims does
 * not overwrite the stored copy. Exercised at the {@link UserMirror} service boundary; the HTTP
 * provisioning path (the interceptor) is covered implicitly by the project-create FK in the CRUD
 * tests.
 */
class UserMirrorIntegrationTest extends AbstractResourceIT {

  @Autowired UserMirror userMirror;
  @Autowired AppUserRepository users;

  @Test
  void provisionsTheMirrorRowOnFirstSight() {
    UUID id = UUID.randomUUID();
    assertThat(users.findById(id)).isEmpty();

    userMirror.ensureProvisioned(id, "first@example.com", true);

    AppUser mirrored = users.findById(id).orElseThrow();
    assertThat(mirrored.email()).isEqualTo("first@example.com");
    assertThat(mirrored.emailVerified()).isTrue();
  }

  @Test
  void doesNotOverwriteAnExistingRowFromALaterCall() {
    UUID id = UUID.randomUUID();

    userMirror.ensureProvisioned(id, "original@example.com", true);
    // a later request for the same subject carrying *changed* identity claims
    userMirror.ensureProvisioned(id, "changed@example.com", false);

    // create-only: the mirror still holds the original identity (update path deferred — ADR-0006)
    AppUser mirrored = users.findById(id).orElseThrow();
    assertThat(mirrored.email()).isEqualTo("original@example.com");
    assertThat(mirrored.emailVerified()).isTrue();
  }
}
