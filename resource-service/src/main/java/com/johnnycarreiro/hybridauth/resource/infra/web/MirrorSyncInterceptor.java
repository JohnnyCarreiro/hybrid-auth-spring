package com.johnnycarreiro.hybridauth.resource.infra.web;

import com.johnnycarreiro.hybridauth.resource.services.UserMirror;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * The mirror-sync seam (ADR-0006 / SDD-002 §8 F-sync): on every authenticated request, ensure the
 * caller has a local {@code app.users} row before the controller runs.
 *
 * <p>Runs after Spring Security has verified the Bearer JWT, so the context holds a {@link
 * JwtAuthenticationToken}; it reads the proven claims ({@code sub}, {@code email}, {@code
 * email_verified}) and hands them to {@link UserMirror#ensureProvisioned} (create-only). Because
 * provisioning commits in its own transaction here in {@code preHandle}, the row is present by the
 * time a {@code POST /projects} tries to satisfy the {@code owner_id} foreign key. Unauthenticated
 * requests (only {@code /health}) carry no token and are skipped.
 */
@Component
public class MirrorSyncInterceptor implements HandlerInterceptor {

  private final UserMirror userMirror;

  public MirrorSyncInterceptor(UserMirror userMirror) {
    this.userMirror = userMirror;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
      Jwt jwt = jwtAuthentication.getToken();
      UUID id = UUID.fromString(jwt.getSubject());
      String email = jwt.getClaimAsString("email");
      Boolean emailVerified = jwt.getClaim("email_verified");
      userMirror.ensureProvisioned(id, email, Boolean.TRUE.equals(emailVerified));
    }
    return true;
  }
}
