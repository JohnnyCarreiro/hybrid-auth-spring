package com.johnnycarreiro.hybridauth.auth.identity.web;

import com.johnnycarreiro.hybridauth.auth.identity.CurrentUser;
import com.johnnycarreiro.hybridauth.auth.identity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Current-user endpoint (SDD-001 §7): {@code GET /me} — the smallest protected route, closing the
 * issue→verify loop (SDD-001 §8 F4).
 *
 * <p>This is the first route behind the Spring Security filter chain. By the time the handler runs,
 * the {@code Authorization: Bearer} access JWT has already been verified locally against the
 * in-process public keys (active + grace, RS256 only — see {@code
 * support.security.SecurityConfig}); a missing, malformed, badly-signed or expired token is
 * rejected by the filter chain with a 401 before this controller is reached. The validated {@link
 * Jwt} is injected as the principal; its {@code sub} resolves to the live {@link User} (or a 401 if
 * that account is gone).
 */
@RestController
public class MeController {

  private final CurrentUser currentUser;

  public MeController(CurrentUser currentUser) {
    this.currentUser = currentUser;
  }

  @GetMapping("/me")
  public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
    User user = currentUser.getMe(jwt.getSubject());
    return ResponseEntity.ok(UserResponse.from(user));
  }
}
