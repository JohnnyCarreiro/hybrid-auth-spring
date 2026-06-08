package com.johnnycarreiro.hybridauth.auth.sessions.web;

import com.johnnycarreiro.hybridauth.auth.sessions.RotateTokenService;
import com.johnnycarreiro.hybridauth.auth.sessions.RotateTokenService.RotatedTokens;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Token-rotation endpoint (SDD-001 §7): {@code POST /auth/token}. The route names the domain
 * intention — rotate a refresh into a fresh pair — and the use case carries the logic. Returns 200
 * + the new hybrid pair; every refresh failure surfaces as a 401 from the 401 family ({@code
 * INVALID_REFRESH}, {@code REFRESH_REUSE_DETECTED}, {@code SESSION_REVOKED}, {@code
 * SESSION_EXPIRED}) via the shared {@code AuthExceptionHandler}, which the client can branch on by
 * {@code code}.
 */
@RestController
@RequestMapping("/auth")
public class TokenController {

  private final RotateTokenService rotateTokenService;

  public TokenController(RotateTokenService rotateTokenService) {
    this.rotateTokenService = rotateTokenService;
  }

  @PostMapping("/token")
  public ResponseEntity<TokenPairResponse> rotate(@Valid @RequestBody RefreshRequest request) {
    RotatedTokens tokens = rotateTokenService.rotate(request.refreshToken());
    return ResponseEntity.ok(TokenPairResponse.from(tokens));
  }
}
