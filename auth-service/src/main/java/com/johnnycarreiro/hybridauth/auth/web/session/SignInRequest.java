package com.johnnycarreiro.hybridauth.auth.web.session;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Inbound body for {@code POST /auth/sign-in}.
 *
 * <p>Edge validation only: {@code @Email}/{@code @NotBlank} reject malformed input with a 400
 * before the use case runs. Credential verification and normalization live in the domain, not here.
 */
public record SignInRequest(@NotBlank @Email String email, @NotBlank String password) {}
