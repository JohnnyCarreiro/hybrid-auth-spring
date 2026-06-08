package com.johnnycarreiro.hybridauth.auth.infra.config;

import com.johnnycarreiro.hybridauth.auth.domain.signing.SigningKeys;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Wires the RS256 issuer (SDD-001 §6): a {@link JWKSource} backed by the live active key and a
 * {@link JwtEncoder} that signs with it.
 *
 * <p>The {@code JWKSource} is queried by the encoder on every mint and resolves to the current
 * active key (private params included) via {@link SigningKeys#activeSigningKey()} — so rotation is
 * picked up automatically without rebuilding the encoder. F3 (sign-in) injects the {@link
 * JwtEncoder} to mint short-lived access tokens; F2 only stands it up (the F2 test mints one token
 * through it to prove grace verification across a rotation).
 */
@Configuration
public class JwksConfig {

  @Bean
  public JWKSource<SecurityContext> jwkSource(SigningKeys keys) {
    return (selector, context) -> selector.select(new JWKSet(keys.activeSigningKey()));
  }

  @Bean
  public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
    return new NimbusJwtEncoder(jwkSource);
  }
}
