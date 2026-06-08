package com.johnnycarreiro.hybridauth.auth.infra.security;

import com.johnnycarreiro.hybridauth.auth.domain.signing.SigningKeys;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The Spring Security filter chain for the auth-service (SDD-001 §6) — introduced by F4 so {@code
 * GET /me} can be protected. Adding the resource-server starter auto-enables security on every
 * route, so this config is also where the already-public surface (sign-up/in, the token + sign-out
 * routes, the JWKS, and the liveness probe) is explicitly permitted; everything else requires a
 * valid Bearer JWT.
 *
 * <p><b>The auth-service is its own resource server.</b> Its protected routes verify the access JWT
 * <em>locally, in-process</em> against the public keys it just minted — no HTTP round-trip to
 * itself, no shared secret (SDD-001 §6 / §4 invariant 6). The {@link #jwtDecoder} bean is built
 * over the servable public set ({@link SigningKeys#publicJwkSet()}, active + grace), restricted to
 * RS256, with Nimbus's default validators (so an expired token is rejected).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * Permit the four public {@code /auth/*} commands, the JWKS document, and the actuator liveness
   * probe; authenticate everything else. The matchers are exact (method + path) so the public
   * surface cannot silently widen.
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.POST, "/auth/sign-up")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/sign-in")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/token")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/sign-out")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/.well-known/jwks.json")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/health", "/health/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .build();
  }

  /**
   * A {@link JwtDecoder} that verifies tokens against the <em>servable public keys</em> (active +
   * grace), RS256 only. Built with the same Nimbus {@code DefaultJWTProcessor} + {@code
   * JWSVerificationKeySelector} construction the JWKS tests use, then wrapped in a {@link
   * NimbusJwtDecoder} so Spring's default validators run (timestamp/exp check) — rejecting expired
   * tokens and bad signatures with a 401.
   *
   * <p>Deliberately does <b>not</b> reuse the issuer {@code JWKSource} from {@code JwksConfig}:
   * that one returns the active signing key <em>with private params</em> and omits in-grace keys,
   * so a token signed just before a rotation would fail to verify. This source returns the public,
   * active-plus-grace set on every resolve, picking up rotation automatically.
   */
  @Bean
  public JwtDecoder jwtDecoder(SigningKeys signingKeys) {
    JWKSource<SecurityContext> publicKeys =
        (selector, context) -> selector.select(signingKeys.publicJwkSet());

    DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
    processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, publicKeys));

    NimbusJwtDecoder decoder = new NimbusJwtDecoder(processor);
    decoder.setJwtValidator(JwtValidators.createDefault());
    return decoder;
  }
}
