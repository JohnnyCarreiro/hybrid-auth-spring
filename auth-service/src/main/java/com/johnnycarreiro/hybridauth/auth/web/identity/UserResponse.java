package com.johnnycarreiro.hybridauth.auth.web.identity;

import com.johnnycarreiro.hybridauth.auth.domain.identity.User;
import java.time.Instant;

/** Public projection of a {@link User} — never carries the password hash. */
public record UserResponse(
    String id, String email, String name, boolean emailVerified, Instant createdAt) {

  public static UserResponse from(User user) {
    return new UserResponse(
        user.id().toString(),
        user.email().value(),
        user.name(),
        user.emailVerified(),
        user.createdAt());
  }
}
