package com.johnnycarreiro.hybridauth.auth.sessions.web;

import com.johnnycarreiro.hybridauth.auth.sessions.SignInService;
import com.johnnycarreiro.hybridauth.auth.sessions.SignInService.SignInResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sign-in endpoint (SDD-001 §7): {@code POST /auth/sign-in}. The route names the domain intention;
 * the use case carries the logic. Returns 200 + hybrid credentials (access JWT + opaque refresh +
 * user); bad creds surface as a 401 {@code INVALID_CREDENTIALS} via the shared {@code
 * AuthExceptionHandler}.
 *
 * <p>Client {@code ip}/{@code userAgent} are read off the request and passed to the use case so the
 * session row carries audit context (used by later reuse-detection diagnostics).
 */
@RestController
@RequestMapping("/auth")
public class SignInController {

  private final SignInService signInService;

  public SignInController(SignInService signInService) {
    this.signInService = signInService;
  }

  @PostMapping("/sign-in")
  public ResponseEntity<AuthTokensResponse> signIn(
      @Valid @RequestBody SignInRequest request, HttpServletRequest http) {
    SignInResult result =
        signInService.signIn(
            request.email(),
            request.password(),
            http.getRemoteAddr(),
            http.getHeader("User-Agent"));
    return ResponseEntity.ok(
        AuthTokensResponse.of(result.accessToken(), result.refreshToken(), result.user()));
  }
}
