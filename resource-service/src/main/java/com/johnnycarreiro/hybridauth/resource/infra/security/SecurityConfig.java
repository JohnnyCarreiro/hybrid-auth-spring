package com.johnnycarreiro.hybridauth.resource.infra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The Spring Security filter chain for the resource-service (SDD-002 §6): a stateless resource
 * server where <strong>every route is protected</strong> except the liveness probe.
 *
 * <p>The JWT is verified locally against the auth-service JWKS by the hand-built {@link
 * JwksTokenVerifier} (a {@link org.springframework.security.oauth2.jwt.JwtDecoder} bean Spring
 * picks up automatically) — no shared secret, no per-request call to auth (ADR-0005). A missing,
 * invalid, or expired Bearer is rejected here as a 401 before any controller runs; ownership-level
 * authorization (404 on another user's resource) is enforced downstream in the services.
 *
 * <p><b>CSRF is disabled deliberately</b>, not by omission (SRS+SAD §2.5): this is a stateless
 * token API with no server-side auth cookie, so a cross-site request carries no ambient credential.
 * CSRF defense lives at the BFF↔browser edge, out of scope here. {@link
 * SessionCreationPolicy#STATELESS} keeps it from ever minting an {@code HttpSession}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.GET, "/health", "/health/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .build();
  }
}
