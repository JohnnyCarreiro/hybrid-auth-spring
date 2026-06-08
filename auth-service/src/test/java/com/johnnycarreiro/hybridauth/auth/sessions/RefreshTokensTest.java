package com.johnnycarreiro.hybridauth.auth.sessions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit checks for the opaque-token mint/hash (no Spring context): tokens are unique and
 * padding-free; the hash is deterministic and never the raw token.
 */
class RefreshTokensTest {

  private final RefreshTokens refreshTokens = new RefreshTokens();

  @Test
  void generate_returns_a_distinct_base64url_token_without_padding_each_call() {
    String a = refreshTokens.generate();
    String b = refreshTokens.generate();

    assertThat(a).isNotEqualTo(b);
    // base64url alphabet only (no +, /, or = padding).
    assertThat(a).matches("[A-Za-z0-9_-]+").doesNotContain("=");
    // 32 bytes → 43 base64 chars without padding.
    assertThat(a).hasSize(43);
  }

  @Test
  void hash_is_deterministic_lowercase_hex_and_not_the_raw_token() {
    String raw = refreshTokens.generate();

    String h1 = refreshTokens.hash(raw);
    String h2 = refreshTokens.hash(raw);

    assertThat(h1).isEqualTo(h2);
    assertThat(h1).isNotEqualTo(raw);
    // SHA-256 → 32 bytes → 64 lowercase hex chars.
    assertThat(h1).matches("[0-9a-f]{64}");
  }
}
