package com.johnnycarreiro.hybridauth.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnnycarreiro.hybridauth.auth.domain.session.Session;
import com.johnnycarreiro.hybridauth.auth.domain.token.RefreshTokens;
import com.johnnycarreiro.hybridauth.auth.infra.database.SessionRepository;
import com.johnnycarreiro.hybridauth.auth.infra.database.UserRepository;
import com.johnnycarreiro.hybridauth.auth.support.AbstractAuthIT;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * F3 acceptance (SDD-001 §8 F3): good creds → 200 with both tokens and a root {@code sessions} row
 * (fresh family, {@code parent_id} NULL, raw refresh not stored); the access JWT verifies against
 * the served JWKS and carries {@code sub}/{@code email}/{@code jti} with a served {@code kid}; bad
 * password and unknown email both → 401 {@code INVALID_CREDENTIALS} (no enumeration).
 */
class SignInIntegrationTest extends AbstractAuthIT {

  private static final String EMAIL = "ada@example.com";
  private static final String PASSWORD = "a-strong-passphrase";

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository users;
  @Autowired SessionRepository sessions;
  @Autowired RefreshTokens refreshTokens;

  private final ObjectMapper json = new ObjectMapper();

  @BeforeEach
  void clean() {
    sessions.deleteAll();
    users.deleteAll();
  }

  private void signUp(String email, String password) throws Exception {
    mockMvc
        .perform(
            post("/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"%s\",\"password\":\"%s\",\"name\":\"Ada\"}"
                        .formatted(email, password)))
        .andExpect(status().isOk());
  }

  private MvcResult signIn(String email, String password) throws Exception {
    return mockMvc
        .perform(
            post("/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
        .andReturn();
  }

  @Test
  void good_credentials_return_both_tokens_and_open_a_root_session_storing_only_the_hash()
      throws Exception {
    signUp(EMAIL, PASSWORD);

    MvcResult result = signIn(EMAIL, PASSWORD);
    assertThat(result.getResponse().getStatus()).isEqualTo(200);

    JsonNode body = json.readTree(result.getResponse().getContentAsString());
    String refreshToken = body.get("refreshToken").asText();
    assertThat(body.get("accessToken").asText()).isNotBlank();
    assertThat(refreshToken).isNotBlank();
    assertThat(body.get("user").get("email").asText()).isEqualTo(EMAIL);

    List<Session> rows = sessions.findAll();
    assertThat(rows).hasSize(1);
    Session row = rows.get(0);
    assertThat(row.parentId()).isNull();
    assertThat(row.familyId()).isNotNull();
    assertThat(row.rotatedAt()).isNull();
    assertThat(row.revokedAt()).isNull();
    // The raw refresh token is NOT stored — only its hash is.
    assertThat(row.tokenHash())
        .isEqualTo(refreshTokens.hash(refreshToken))
        .isNotEqualTo(refreshToken);
  }

  @Test
  void access_token_verifies_against_the_jwks_and_carries_sub_email_and_jti() throws Exception {
    signUp(EMAIL, PASSWORD);

    MvcResult signIn = signIn(EMAIL, PASSWORD);
    JsonNode body = json.readTree(signIn.getResponse().getContentAsString());
    String accessToken = body.get("accessToken").asText();
    String userId = body.get("user").get("id").asText();

    // Build a decoder from the *served* public JWKS, exactly as the resource-service will.
    String jwksJson =
        mockMvc
            .perform(get("/.well-known/jwks.json"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JWKSet served = JWKSet.parse(jwksJson);

    DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
    processor.setJWSKeySelector(
        new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, new ImmutableJWKSet<>(served)));
    JwtDecoder decoder = new NimbusJwtDecoder(processor);

    Jwt verified = decoder.decode(accessToken);
    assertThat(verified.getSubject()).isEqualTo(userId);
    assertThat(verified.getClaimAsString("email")).isEqualTo(EMAIL);
    assertThat(verified.getId()).isNotBlank(); // jti
    // The JWS header kid is one served by the JWKS.
    String kid = (String) verified.getHeaders().get("kid");
    assertThat(served.getKeyByKeyId(kid)).isNotNull();
  }

  @Test
  void wrong_password_returns_401_invalid_credentials() throws Exception {
    signUp(EMAIL, PASSWORD);

    mockMvc
        .perform(
            post("/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, "wrong-password")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }

  @Test
  void unknown_email_returns_the_same_401_invalid_credentials() throws Exception {
    // No sign-up: the email is unknown. Same code as a wrong password → no enumeration.
    mockMvc
        .perform(
            post("/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"%s\",\"password\":\"%s\"}"
                        .formatted("ghost@example.com", PASSWORD)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }
}
