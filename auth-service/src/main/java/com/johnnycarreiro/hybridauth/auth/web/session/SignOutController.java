package com.johnnycarreiro.hybridauth.auth.web.session;

import com.johnnycarreiro.hybridauth.auth.services.SignOutService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sign-out endpoint (SDD-001 §7): {@code POST /auth/sign-out}. Authenticated by possession of the
 * opaque refresh token in the body (not a Bearer JWT), so it is part of the public surface in
 * {@code SecurityConfig}. Returns 204 — the session is revoked and there is nothing to return.
 * Reuses the F5 {@link RefreshRequest} body.
 */
@RestController
@RequestMapping("/auth")
public class SignOutController {

  private final SignOutService signOutService;

  public SignOutController(SignOutService signOutService) {
    this.signOutService = signOutService;
  }

  @PostMapping("/sign-out")
  public ResponseEntity<Void> signOut(@Valid @RequestBody RefreshRequest request) {
    signOutService.signOut(request.refreshToken());
    return ResponseEntity.noContent().build();
  }
}
