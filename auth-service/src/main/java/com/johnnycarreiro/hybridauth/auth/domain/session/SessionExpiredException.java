package com.johnnycarreiro.hybridauth.auth.domain.session;

import com.johnnycarreiro.hybridauth.auth.domain.shared.AuthErrorCode;
import com.johnnycarreiro.hybridauth.auth.domain.shared.AuthException;

/**
 * The presented session's window has closed (SDD-001 §5 → HTTP 401).
 *
 * <p>Its {@code expiresAt} is at or before now: the sliding refresh window lapsed without a
 * rotation, so the session can no longer be exchanged. Unlike reuse, an honest expiry does not kill
 * the family — it simply requires a fresh login.
 */
public class SessionExpiredException extends AuthException {

  public SessionExpiredException() {
    super(AuthErrorCode.SESSION_EXPIRED, "session has expired");
  }
}
