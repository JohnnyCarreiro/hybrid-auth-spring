package com.johnnycarreiro.hybridauth.auth.domain.session;

import com.johnnycarreiro.hybridauth.auth.domain.shared.AuthErrorCode;
import com.johnnycarreiro.hybridauth.auth.domain.shared.AuthException;

/**
 * The presented refresh token matched no session at all (SDD-001 §5 → HTTP 401).
 *
 * <p>The hash of the supplied token had no row in {@code sessions}: it was never issued, was
 * already garbage-collected, or is forged. As with sign-in, the message names neither the token nor
 * the reason — it carries no signal an attacker could mine.
 */
public class InvalidRefreshException extends AuthException {

  public InvalidRefreshException() {
    super(AuthErrorCode.INVALID_REFRESH, "invalid refresh token");
  }
}
