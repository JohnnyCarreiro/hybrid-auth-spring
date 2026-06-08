package com.johnnycarreiro.hybridauth.auth.domain.signing;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM seal for signing-key private material at rest (SDD-001 §4 invariant 6).
 *
 * <p>The private half of every RS256 key pair is persisted only as ciphertext produced here; it
 * never touches the {@code jwks} table in clear and never leaves the service. The data-encryption
 * key is a single base64-encoded 32-byte secret read from {@code auth.jwks.encryption-key} (env
 * {@code AUTH_JWKS_ENC_KEY}) — out of the codebase, out of the repo.
 *
 * <p>Wire format of a sealed value: {@code base64( IV(12 bytes) || ciphertext+tag )}. A fresh
 * random 12-byte IV is generated per encryption (GCM must never reuse an IV under one key) and
 * prepended so decryption is self-contained; the GCM authentication tag is 128 bits, appended by
 * the cipher to the ciphertext. Decryption splits the IV back off and verifies the tag — any
 * tampering fails the open.
 */
@Component
public class PrivateKeyCipher {

  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int KEY_BYTES = 32; // AES-256
  private static final int IV_BYTES = 12; // GCM standard nonce length
  private static final int TAG_BITS = 128;

  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();

  public PrivateKeyCipher(@Value("${auth.jwks.encryption-key:}") String encodedKey) {
    if (encodedKey == null || encodedKey.isBlank()) {
      throw new IllegalStateException(
          "auth.jwks.encryption-key (env AUTH_JWKS_ENC_KEY) is required: a base64-encoded 32-byte"
              + " AES key, e.g. `openssl rand -base64 32`");
    }
    byte[] raw;
    try {
      raw = Base64.getDecoder().decode(encodedKey.trim());
    } catch (IllegalArgumentException notBase64) {
      throw new IllegalStateException(
          "auth.jwks.encryption-key must be valid base64 of 32 bytes (got non-base64 input)",
          notBase64);
    }
    if (raw.length != KEY_BYTES) {
      throw new IllegalStateException(
          "auth.jwks.encryption-key must decode to exactly "
              + KEY_BYTES
              + " bytes (AES-256); got "
              + raw.length);
    }
    this.key = new SecretKeySpec(raw, "AES");
  }

  /** Seal UTF-8 plaintext, returning {@code base64(IV || ciphertext+tag)}. */
  public String encrypt(String plaintext) {
    try {
      byte[] iv = new byte[IV_BYTES];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      byte[] out = new byte[iv.length + ciphertext.length];
      System.arraycopy(iv, 0, out, 0, iv.length);
      System.arraycopy(ciphertext, 0, out, iv.length, ciphertext.length);
      return Base64.getEncoder().encodeToString(out);
    } catch (Exception e) {
      throw new IllegalStateException("failed to encrypt private key material", e);
    }
  }

  /**
   * Open a value produced by {@link #encrypt}, verifying the GCM tag; returns the UTF-8 plaintext.
   */
  public String decrypt(String sealed) {
    try {
      byte[] in = Base64.getDecoder().decode(sealed);
      if (in.length <= IV_BYTES) {
        throw new IllegalArgumentException(
            "sealed value too short to contain a " + IV_BYTES + "-byte IV plus ciphertext");
      }
      byte[] iv = new byte[IV_BYTES];
      System.arraycopy(in, 0, iv, 0, IV_BYTES);
      byte[] ciphertext = new byte[in.length - IV_BYTES];
      System.arraycopy(in, IV_BYTES, ciphertext, 0, ciphertext.length);
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("failed to decrypt private key material", e);
    }
  }
}
