package com.johnnycarreiro.hybridauth.resource.services;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated caller's id from the verified access token (SDD-002 §3).
 *
 * <p>By the time a controller calls this, Spring Security's resource-server filter has already
 * verified the JWT (signature/expiry via {@code JwksTokenVerifier}) and put a {@link
 * JwtAuthenticationToken} in the context. This only reads the proven {@code sub} claim — the auth
 * user id — which is the ownership key for every project/task operation. It is the resource-service
 * analogue of the auth-service's {@code CurrentUser}.
 */
@Component
public class CurrentUser {

  /**
   * The verified caller's id (the access-token {@code sub}).
   *
   * @throws IllegalStateException if there is no authenticated JWT in context (a routing bug —
   *     every non-{@code /health} route is authenticated) or its {@code sub} is not a UUID (a token
   *     this service did not contractually expect).
   */
  public UUID requireId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
      throw new IllegalStateException("no authenticated JWT in the security context");
    }
    Jwt jwt = jwtAuthentication.getToken();
    try {
      return UUID.fromString(jwt.getSubject());
    } catch (IllegalArgumentException malformedSubject) {
      throw new IllegalStateException("access token sub is not a UUID: " + jwt.getSubject());
    }
  }
}
