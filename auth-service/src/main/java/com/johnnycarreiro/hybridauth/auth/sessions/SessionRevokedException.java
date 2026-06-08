package com.johnnycarreiro.hybridauth.auth.sessions;

import com.johnnycarreiro.hybridauth.auth.support.error.AuthErrorCode;
import com.johnnycarreiro.hybridauth.auth.support.error.AuthException;

/**
 * The presented session was explicitly revoked (SDD-001 §5 → HTTP 401).
 *
 * <p>Its {@code revokedAt} is set — by a sign-out (F6) or because an earlier reuse killed the whole
 * family. A revoked session is dead and cannot be rotated; the caller must sign in again.
 */
public class SessionRevokedException extends AuthException {

  public SessionRevokedException() {
    super(AuthErrorCode.SESSION_REVOKED, "session has been revoked");
  }
}
