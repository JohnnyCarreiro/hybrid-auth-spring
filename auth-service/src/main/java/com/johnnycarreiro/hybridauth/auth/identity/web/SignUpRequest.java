package com.johnnycarreiro.hybridauth.auth.identity.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound body for {@code POST /auth/sign-up}.
 *
 * <p>Edge validation only: {@code @Email}/{@code @NotBlank} reject malformed input with a 400
 * before the use case runs. The substantive rules (normalization, password policy, uniqueness) live
 * in the domain, not here.
 */
public record SignUpRequest(
    @NotBlank @Email String email, @NotBlank String password, @Size(max = 100) String name) {}
