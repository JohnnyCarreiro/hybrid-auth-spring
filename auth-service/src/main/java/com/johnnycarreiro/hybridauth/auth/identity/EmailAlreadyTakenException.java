package com.johnnycarreiro.hybridauth.auth.identity;

import com.johnnycarreiro.hybridauth.auth.support.error.AuthErrorCode;
import com.johnnycarreiro.hybridauth.auth.support.error.AuthException;

/** Sign-up attempted with an email that already has an account (SDD-001 §5 → HTTP 409). */
public class EmailAlreadyTakenException extends AuthException {

  public EmailAlreadyTakenException(String email) {
    super(AuthErrorCode.EMAIL_ALREADY_TAKEN, "email already registered: " + email);
  }
}
