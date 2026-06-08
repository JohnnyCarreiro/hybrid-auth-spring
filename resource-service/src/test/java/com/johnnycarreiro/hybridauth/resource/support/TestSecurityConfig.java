package com.johnnycarreiro.hybridauth.resource.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Test wiring that lets the CRUD/ownership integration tests exercise the real security filter
 * chain without a live auth-service: a {@code @Primary} {@link JwtDecoder} that trusts a fixed
 * in-test RSA key (so {@link
 * com.johnnycarreiro.hybridauth.resource.infra.security.JwksTokenVerifier} — which would fetch a
 * remote JWKS — is shadowed), plus a {@link TestTokens} minter for that key.
 *
 * <p>The verifier's own refetch-on-rotation behavior is covered separately and directly in {@code
 * JwksTokenVerifierTest}; here we care about authorization, not key fetching.
 */
@TestConfiguration
public class TestSecurityConfig {

  /** A fixed RSA key shared by the test decoder (verify) and {@link TestTokens} (sign). */
  public static final RSAKey TEST_KEY = generate();

  private static RSAKey generate() {
    try {
      return new RSAKeyGenerator(2048).keyID("test-signing-key").generate();
    } catch (JOSEException e) {
      throw new IllegalStateException("failed to generate test RSA key", e);
    }
  }

  @Bean
  @Primary
  JwtDecoder testJwtDecoder() throws JOSEException {
    return NimbusJwtDecoder.withPublicKey(TEST_KEY.toRSAPublicKey()).build();
  }

  @Bean
  TestTokens testTokens() {
    return new TestTokens(TEST_KEY);
  }
}
