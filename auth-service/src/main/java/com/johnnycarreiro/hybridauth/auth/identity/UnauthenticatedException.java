package com.johnnycarreiro.hybridauth.auth.identity;

import com.johnnycarreiro.hybridauth.auth.support.error.AuthErrorCode;
import com.johnnycarreiro.hybridauth.auth.support.error.AuthException;

/**
 * The presented access JWT verified, but the subject it names is no longer a live account (SDD-001
 * §5 {@code Unauthenticated} → HTTP 401).
 *
 * <p>This is the application-level half of "unauthenticated": a structurally valid, in-date,
 * correctly-signed token whose {@code sub} resolves to nothing (e.g. the user was deleted after the
 * token was minted). A <em>missing</em> or <em>malformed</em> token never reaches the use case —
 * Spring Security's filter chain rejects it with its own 401 before the controller runs. Both cases
 * are deliberately the same status and code so a caller cannot tell a deleted account apart from a
 * bad token.
 */
public class UnauthenticatedException extends AuthException {

  public UnauthenticatedException() {
    super(AuthErrorCode.UNAUTHENTICATED, "not authenticated");
  }
}
