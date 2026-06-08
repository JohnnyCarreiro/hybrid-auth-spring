package com.johnnycarreiro.hybridauth.auth.domain.identity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Unit coverage for the registration password policy. */
class PasswordPolicyTest {

  @Test
  void accepts_a_password_at_or_above_the_minimum_length() {
    assertThatCode(() -> PasswordPolicy.check("a-strong-passphrase")).doesNotThrowAnyException();
  }

  @Test
  void rejects_a_password_below_the_minimum_length() {
    assertThatThrownBy(() -> PasswordPolicy.check("short"))
        .isInstanceOf(WeakPasswordException.class)
        .hasMessageContaining(String.valueOf(PasswordPolicy.MIN_LENGTH));
  }

  @Test
  void rejects_a_null_password() {
    assertThatThrownBy(() -> PasswordPolicy.check(null)).isInstanceOf(WeakPasswordException.class);
  }

  @Test
  void rejects_a_password_above_the_maximum_length() {
    String tooLong = "x".repeat(PasswordPolicy.MAX_LENGTH + 1);
    assertThatThrownBy(() -> PasswordPolicy.check(tooLong))
        .isInstanceOf(WeakPasswordException.class)
        .hasMessageContaining(String.valueOf(PasswordPolicy.MAX_LENGTH));
  }
}
