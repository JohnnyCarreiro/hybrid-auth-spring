package com.johnnycarreiro.hybridauth.auth.services;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnnycarreiro.hybridauth.auth.infra.database.UserRepository;
import com.johnnycarreiro.hybridauth.auth.support.AbstractAuthIT;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * F4 acceptance (SDD-001 §8 F4): {@code GET /me} returns the authenticated user for a valid access
 * JWT, and 401s on every failure path — missing token, malformed token, a token signed by a foreign
 * key (bad signature), and an expired token. Also pins that introducing the Spring Security filter
 * chain left the existing public routes (sign-up / sign-in / JWKS) reachable.
 */
class MeIntegrationTest extends AbstractAuthIT {

  private static final String EMAIL = "grace@example.com";
  private static final String PASSWORD = "a-strong-passphrase";

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository users;
  @Autowired JwtEncoder jwtEncoder;

  private final ObjectMapper json = new ObjectMapper();

  @BeforeEach
  void clean() {
    users.deleteAll();
  }

  private void signUp() throws Exception {
    mockMvc
        .perform(
            post("/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"%s\",\"password\":\"%s\",\"name\":\"Grace\"}"
                        .formatted(EMAIL, PASSWORD)))
        .andExpect(status().isOk());
  }

  /** Sign up + sign in against the real endpoints, returning the parsed sign-in body. */
  private JsonNode signUpAndSignIn() throws Exception {
    signUp();
    MvcResult signIn =
        mockMvc
            .perform(
                post("/auth/sign-in")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();
    return json.readTree(signIn.getResponse().getContentAsString());
  }

  @Test
  void valid_bearer_token_returns_200_and_the_current_user() throws Exception {
    JsonNode signIn = signUpAndSignIn();
    String accessToken = signIn.get("accessToken").asText();
    String userId = signIn.get("user").get("id").asText();

    mockMvc
        .perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId))
        .andExpect(jsonPath("$.email").value(EMAIL));
  }

  @Test
  void missing_token_returns_401() throws Exception {
    mockMvc.perform(get("/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void malformed_token_returns_401() throws Exception {
    mockMvc
        .perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void token_signed_by_a_foreign_key_returns_401() throws Exception {
    // A structurally valid RS256 JWT, but signed by a key the JWKS never served → bad signature.
    RSAKey foreignKey = generateRsaKey();
    JwtEncoder foreignEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(foreignKey)));

    Instant now = Instant.now();
    String token =
        foreignEncoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(SignatureAlgorithm.RS256).keyId(foreignKey.getKeyID()).build(),
                    JwtClaimsSet.builder()
                        .subject(UUID.randomUUID().toString())
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(900))
                        .build()))
            .getTokenValue();

    mockMvc
        .perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void expired_token_signed_by_the_active_key_returns_401() throws Exception {
    // Mint with the real issuer encoder (active key) but with exp in the past → rejected on exp.
    Instant past = Instant.now().minusSeconds(120);
    String expired =
        jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    JwtClaimsSet.builder()
                        .subject(UUID.randomUUID().toString())
                        .issuedAt(past)
                        .expiresAt(past.plusSeconds(60)) // expired 60s ago
                        .build()))
            .getTokenValue();

    mockMvc
        .perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void public_routes_remain_reachable_under_the_security_filter_chain() throws Exception {
    // Sign-up (POST /auth/sign-up) and JWKS (GET /.well-known/jwks.json) must stay public.
    signUp();
    mockMvc.perform(get("/.well-known/jwks.json")).andExpect(status().isOk());
  }

  private static RSAKey generateRsaKey() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      KeyPair pair = generator.generateKeyPair();
      return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
          .privateKey((RSAPrivateKey) pair.getPrivate())
          .keyID(UUID.randomUUID().toString())
          .algorithm(JWSAlgorithm.RS256)
          .keyUse(KeyUse.SIGNATURE)
          .build();
    } catch (Exception e) {
      throw new IllegalStateException("failed to generate a foreign RSA key for the test", e);
    }
  }
}
