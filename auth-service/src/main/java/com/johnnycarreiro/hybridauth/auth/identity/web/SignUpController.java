package com.johnnycarreiro.hybridauth.auth.identity.web;

import com.johnnycarreiro.hybridauth.auth.identity.SignUpService;
import com.johnnycarreiro.hybridauth.auth.identity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sign-up endpoint. The route names the domain intention (SDD-001 §7); the use case carries the
 * logic. Returns 200 + the created user — no token at MVP (sign-in is a separate step, F3).
 */
@RestController
@RequestMapping("/auth")
public class SignUpController {

  private final SignUpService signUpService;

  public SignUpController(SignUpService signUpService) {
    this.signUpService = signUpService;
  }

  @PostMapping("/sign-up")
  public ResponseEntity<UserResponse> signUp(@Valid @RequestBody SignUpRequest request) {
    User user = signUpService.signUp(request.email(), request.password(), request.name());
    return ResponseEntity.ok(UserResponse.from(user));
  }
}
