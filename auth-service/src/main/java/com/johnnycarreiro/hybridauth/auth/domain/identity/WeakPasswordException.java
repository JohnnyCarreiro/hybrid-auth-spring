package com.johnnycarreiro.hybridauth.auth.domain.identity;

import com.johnnycarreiro.hybridauth.auth.domain.shared.AuthErrorCode;
import com.johnnycarreiro.hybridauth.auth.domain.shared.AuthException;

/** A submitted password failed {@link PasswordPolicy} (SDD-001 §5 → HTTP 422). */
public class WeakPasswordException extends AuthException {

  public WeakPasswordException(String reason) {
    super(AuthErrorCode.WEAK_PASSWORD, "weak password: " + reason);
  }
}
