package com.johnnycarreiro.hybridauth.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnnycarreiro.hybridauth.auth.domain.token.RefreshTokens;
import com.johnnycarreiro.hybridauth.auth.infra.database.SessionRepository;
import com.johnnycarreiro.hybridauth.auth.support.AbstractAuthIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * F6 acceptance (SDD-001 §8): sign-out revokes the presented session; a later rotate on it → 401
 * SESSION_REVOKED; an unknown token → 401 INVALID_REFRESH; sign-out is idempotent.
 */
class SignOutIntegrationTest extends AbstractAuthIT {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper json;
  @Autowired SessionRepository sessions;
  @Autowired RefreshTokens refreshTokens;

  @BeforeEach
  void clean() {
    sessions.deleteAll();
  }

  /** Sign up + sign in a fresh user; return the raw refresh token. */
  private String signUpAndSignIn(String email) throws Exception {
    mockMvc
        .perform(
            post("/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"%s\",\"password\":\"a-strong-passphrase\",\"name\":\"Ada\"}"
                        .formatted(email)))
        .andExpect(status().isOk());
    String body =
        mockMvc
            .perform(
                post("/auth/sign-in")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"%s\",\"password\":\"a-strong-passphrase\"}".formatted(email)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(body).get("refreshToken").asText();
  }

  private static String refreshBody(String refreshToken) {
    return "{\"refreshToken\":\"%s\"}".formatted(refreshToken);
  }

  @Test
  void sign_out_revokes_the_session_and_a_later_rotate_is_401_session_revoked() throws Exception {
    String refresh = signUpAndSignIn("signout@example.com");

    mockMvc
        .perform(
            post("/auth/sign-out")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(refresh)))
        .andExpect(status().isNoContent());

    // The presented session is revoked at rest.
    assertThat(sessions.findAll())
        .singleElement()
        .satisfies(s -> assertThat(s.revokedAt()).isNotNull());

    // Rotating the now-revoked token is rejected.
    mockMvc
        .perform(
            post("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(refresh)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("SESSION_REVOKED"));
  }

  @Test
  void sign_out_with_an_unknown_token_is_401_invalid_refresh() throws Exception {
    mockMvc
        .perform(
            post("/auth/sign-out")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody("not-a-real-token")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_REFRESH"));
  }

  @Test
  void sign_out_is_idempotent() throws Exception {
    String refresh = signUpAndSignIn("idempotent@example.com");

    mockMvc
        .perform(
            post("/auth/sign-out")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(refresh)))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            post("/auth/sign-out")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(refresh)))
        .andExpect(status().isNoContent());
  }
}
