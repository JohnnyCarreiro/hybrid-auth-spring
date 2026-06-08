package com.johnnycarreiro.hybridauth.auth.jwks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

/** Unit cover for the AES-256-GCM seal: round-trip, ciphertext opacity, and key-shape guards. */
class PrivateKeyCipherTest {

  private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

  @Test
  void round_trips_plaintext() {
    PrivateKeyCipher cipher = new PrivateKeyCipher(KEY);
    String plaintext = "{\"kty\":\"RSA\",\"d\":\"secret\"}";

    String sealed = cipher.encrypt(plaintext);

    assertThat(sealed).isNotEqualTo(plaintext).doesNotContain("kty").doesNotContain("secret");
    assertThat(cipher.decrypt(sealed)).isEqualTo(plaintext);
  }

  @Test
  void distinct_iv_yields_distinct_ciphertext_for_same_input() {
    PrivateKeyCipher cipher = new PrivateKeyCipher(KEY);

    assertThat(cipher.encrypt("same")).isNotEqualTo(cipher.encrypt("same"));
  }

  @Test
  void rejects_a_missing_key() {
    assertThatThrownBy(() -> new PrivateKeyCipher(""))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("required");
  }

  @Test
  void rejects_a_wrong_length_key() {
    String sixteenBytes = Base64.getEncoder().encodeToString(new byte[16]);

    assertThatThrownBy(() -> new PrivateKeyCipher(sixteenBytes))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("32 bytes");
  }
}
