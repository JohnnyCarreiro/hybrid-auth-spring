package com.johnnycarreiro.hybridauth.auth.domain.session;

import com.johnnycarreiro.hybridauth.auth.domain.shared.AuthErrorCode;
import com.johnnycarreiro.hybridauth.auth.domain.shared.AuthException;

/**
 * A refresh token that was <em>already rotated</em> was presented again (SDD-001 §4 invariant 2 →
 * HTTP 401) — the stolen-refresh signal.
 *
 * <p>Once a refresh is exchanged, its session gets a {@code rotatedAt} stamp; that token must never
 * be valid again. Seeing it a second time means two parties hold the same secret (a leak/theft), so
 * the rule is scorched-earth: the <strong>whole family is revoked</strong> before this is thrown,
 * killing the attacker's chain <em>and</em> the victim's, forcing a fresh login. The revoke runs
 * inline in the rotation transaction, which is declared {@code noRollbackFor} this exception so the
 * family death is committed even though throwing this signals the 401 (see {@code
 * RotateTokenService}).
 */
public class ReuseDetectedException extends AuthException {

  public ReuseDetectedException() {
    super(
        AuthErrorCode.REFRESH_REUSE_DETECTED,
        "refresh token reuse detected; session family revoked");
  }
}
