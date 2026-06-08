package com.johnnycarreiro.hybridauth.auth.web.session;

import com.johnnycarreiro.hybridauth.auth.services.RotateTokenService.RotatedTokens;

/**
 * The hybrid-credentials body returned by {@code POST /auth/token}: the freshly minted RS256 access
 * JWT and the new opaque refresh token.
 *
 * <p>Unlike sign-in's response this carries no user projection — a rotation is a credential
 * refresh, not an identity lookup. The refresh token is the <em>raw</em> value, returned exactly
 * once and never persisted in clear (SDD-001 §4 invariant 7).
 */
public record TokenPairResponse(String accessToken, String refreshToken) {

  public static TokenPairResponse from(RotatedTokens tokens) {
    return new TokenPairResponse(tokens.accessToken(), tokens.refreshToken());
  }
}
