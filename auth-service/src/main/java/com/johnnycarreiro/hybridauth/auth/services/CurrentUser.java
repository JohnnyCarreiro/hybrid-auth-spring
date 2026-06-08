package com.johnnycarreiro.hybridauth.auth.services;

import com.johnnycarreiro.hybridauth.auth.domain.identity.UnauthenticatedException;
import com.johnnycarreiro.hybridauth.auth.domain.identity.User;
import com.johnnycarreiro.hybridauth.auth.infra.database.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The {@code getMe} use case (SDD-001 §3): resolve the authenticated subject to its {@link User}.
 *
 * <p>The caller (the web edge) has already had Spring Security verify the access JWT — signature,
 * expiry, RS256 — against the in-process public keys; this service only translates the proven
 * {@code sub} claim into the live aggregate. If the subject no longer exists (the account was
 * deleted after the token was minted) it is an {@link UnauthenticatedException} (401),
 * indistinguishable to the caller from a bad token.
 */
@Service
public class CurrentUser {

  private final UserRepository users;

  public CurrentUser(UserRepository users) {
    this.users = users;
  }

  /**
   * Load the {@link User} named by a verified token {@code sub} claim.
   *
   * @param subject the {@code sub} claim of an already-verified access JWT (a UUID string)
   * @return the live account for that subject
   * @throws UnauthenticatedException if {@code subject} is not a UUID or names no live account
   */
  @Transactional(readOnly = true)
  public User getMe(String subject) {
    UUID id = parseSubject(subject);
    return users.findById(id).orElseThrow(UnauthenticatedException::new);
  }

  private UUID parseSubject(String subject) {
    try {
      return UUID.fromString(subject);
    } catch (IllegalArgumentException malformedSubject) {
      // A verified token with a non-UUID sub is not one we issued — treat as unauthenticated.
      throw new UnauthenticatedException();
    }
  }
}
