package com.johnnycarreiro.hybridauth.auth.domain.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * An email address as a normalized value object owned by {@link User}.
 *
 * <p>Normalization (trim + lowercase) lives here, not in a service, so two spellings of the same
 * address can never produce two accounts — the rule travels with the value wherever it is used.
 * Edge-level format checking is handled by bean validation on the request DTO ({@code @Email});
 * this type's guard is the last-line defense that a blank value can never be persisted.
 */
@Embeddable
public class Email {

  @Column(name = "email", nullable = false, unique = true)
  private String value;

  protected Email() {
    // JPA
  }

  private Email(String value) {
    this.value = value;
  }

  /** Build a normalized email, rejecting blank input. */
  public static Email of(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("email must not be blank");
    }
    return new Email(raw.trim().toLowerCase());
  }

  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Email other)) {
      return false;
    }
    return Objects.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
