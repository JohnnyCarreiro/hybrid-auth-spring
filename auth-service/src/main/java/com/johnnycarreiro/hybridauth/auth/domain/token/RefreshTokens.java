package com.johnnycarreiro.hybridauth.auth.domain.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Mint and hash opaque refresh tokens (SDD-001 §4 invariant 7).
 *
 * <p>A refresh token is an <em>opaque</em> high-entropy secret — not a JWT — generated from {@link
 * SecureRandom} and handed to the client once. It is never stored in clear: the persisted {@code
 * sessions.token_hash} is its {@link #hash(String) SHA-256 digest}, so a database read can confirm
 * a presented token (by re-hashing and matching) without ever holding the secret. The raw value is
 * returned to the caller exactly once and never logged.
 *
 * <p><b>Encodings.</b> {@link #generate()} returns 32 random bytes (256 bits) as base64url
 * <em>without</em> padding — URL/header-safe and free of {@code =} noise. {@link #hash(String)}
 * returns the SHA-256 digest as lowercase hex (a stable, fixed-width column value).
 */
@Component
public class RefreshTokens {

  private static final int TOKEN_BYTES = 32;
  private static final char[] HEX = "0123456789abcdef".toCharArray();

  private final SecureRandom random = new SecureRandom();

  /** A fresh opaque refresh token: 32 random bytes, base64url-encoded, no padding. */
  public String generate() {
    byte[] bytes = new byte[TOKEN_BYTES];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /** The SHA-256 digest of the raw token's UTF-8 bytes, as lowercase hex — what gets persisted. */
  public String hash(String rawToken) {
    byte[] digest = sha256(rawToken.getBytes(StandardCharsets.UTF_8));
    StringBuilder hex = new StringBuilder(digest.length * 2);
    for (byte b : digest) {
      hex.append(HEX[(b >> 4) & 0xF]).append(HEX[b & 0xF]);
    }
    return hex.toString();
  }

  private static byte[] sha256(byte[] input) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(input);
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 is mandated by every JVM; absence is an unrecoverable environment fault.
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
