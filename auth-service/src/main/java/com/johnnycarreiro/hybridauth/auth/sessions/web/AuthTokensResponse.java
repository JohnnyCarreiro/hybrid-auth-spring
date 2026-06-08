package com.johnnycarreiro.hybridauth.auth.sessions.web;

import com.johnnycarreiro.hybridauth.auth.identity.User;
import com.johnnycarreiro.hybridauth.auth.identity.web.UserResponse;

/**
 * The hybrid-credentials body returned by {@code POST /auth/sign-in} (and later by token rotation):
 * the RS256 access JWT, the opaque refresh token, and a public projection of the user.
 *
 * <p>The refresh token is the <em>raw</em> value — returned exactly once here and never persisted
 * in clear (SDD-001 §4 invariant 7). The user projection reuses {@link UserResponse}, which never
 * carries the password hash.
 */
public record AuthTokensResponse(String accessToken, String refreshToken, UserResponse user) {

  public static AuthTokensResponse of(String accessToken, String refreshToken, User user) {
    return new AuthTokensResponse(accessToken, refreshToken, UserResponse.from(user));
  }
}
