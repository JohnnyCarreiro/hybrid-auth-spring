package com.johnnycarreiro.hybridauth.auth.domain.session;

import com.johnnycarreiro.hybridauth.auth.domain.shared.AuthErrorCode;
import com.johnnycarreiro.hybridauth.auth.domain.shared.AuthException;

/**
 * Sign-in failed: the email is unknown <em>or</em> the password did not match (SDD-001 §5 → HTTP
 * 401).
 *
 * <p>The two cases are deliberately indistinguishable to the caller — one code, one message, no
 * hint which half failed — so the endpoint cannot be used to enumerate registered emails (SDD-001
 * §8 F3 validation). The message names neither the email nor the reason.
 */
public class InvalidCredentialsException extends AuthException {

  public InvalidCredentialsException() {
    super(AuthErrorCode.INVALID_CREDENTIALS, "invalid email or password");
  }
}
