package com.johnnycarreiro.hybridauth.auth.identity;

import com.johnnycarreiro.hybridauth.auth.support.error.AuthErrorCode;
import com.johnnycarreiro.hybridauth.auth.support.error.AuthException;

/** A submitted password failed {@link PasswordPolicy} (SDD-001 §5 → HTTP 422). */
public class WeakPasswordException extends AuthException {

  public WeakPasswordException(String reason) {
    super(AuthErrorCode.WEAK_PASSWORD, "weak password: " + reason);
  }
}
