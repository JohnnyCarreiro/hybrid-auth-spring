package com.johnnycarreiro.hybridauth.auth.sessions.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound body for {@code POST /auth/token}: the opaque refresh token to exchange.
 *
 * <p>Edge validation only — {@code @NotBlank} rejects an empty/missing token with a 400 before the
 * rotation use case runs. All token semantics (hashing, reuse-detection) live in the domain.
 */
public record RefreshRequest(@NotBlank String refreshToken) {}
