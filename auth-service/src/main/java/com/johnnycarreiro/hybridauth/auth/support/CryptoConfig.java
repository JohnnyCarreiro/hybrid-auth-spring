package com.johnnycarreiro.hybridauth.auth.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cryptographic beans for the auth domain.
 *
 * <p>Password hashing is Argon2id (ADR-0002) via Spring Security's {@link Argon2PasswordEncoder},
 * which is backed by BouncyCastle. The {@code defaultsForSpringSecurity_v5_8()} preset is Argon2id
 * with current OWASP-aligned parameters (m=19456 KiB, t=2, p=1, 16-byte salt, 32-byte hash); the
 * encoded output embeds those parameters, so a future hardening of the cost stays verifiable
 * against existing hashes.
 *
 * <p>Only the hash is ever persisted — the raw password is never stored or logged (SDD-001 §4,
 * invariant 7).
 */
@Configuration
public class CryptoConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
  }
}
