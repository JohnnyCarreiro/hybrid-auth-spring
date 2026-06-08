package com.johnnycarreiro.hybridauth.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.johnnycarreiro.hybridauth.auth.domain.identity.User;
import com.johnnycarreiro.hybridauth.auth.infra.database.UserRepository;
import com.johnnycarreiro.hybridauth.auth.support.AbstractAuthIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * F1 acceptance (SDD-001 §8): valid → 200 + user; duplicate → 409; weak → 422; hash never clear.
 */
class SignUpIntegrationTest extends AbstractAuthIT {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository users;

  @BeforeEach
  void clean() {
    users.deleteAll();
  }

  private static String body(String email, String password) {
    return "{\"email\":\"%s\",\"password\":\"%s\",\"name\":\"Ada\"}".formatted(email, password);
  }

  @Test
  void valid_signup_returns_200_and_persists_an_argon2id_hash_not_the_clear_password()
      throws Exception {
    String password = "a-strong-passphrase";

    mockMvc
        .perform(
            post("/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("Ada@Example.com", password)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.email").value("ada@example.com")) // normalized
        .andExpect(jsonPath("$.emailVerified").value(false));

    User stored = users.findAll().get(0);
    assertThat(stored.passwordHash()).startsWith("$argon2id$").isNotEqualTo(password);
  }

  @Test
  void duplicate_email_returns_409_with_email_already_taken_code() throws Exception {
    mockMvc
        .perform(
            post("/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("dupe@example.com", "a-strong-passphrase")))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body("DUPE@example.com", "another-strong-pass"))) // same email, different case
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_TAKEN"));
  }

  @Test
  void weak_password_returns_422_with_weak_password_code() throws Exception {
    mockMvc
        .perform(
            post("/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("weak@example.com", "short")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));
  }

  @Test
  void malformed_email_returns_400() throws Exception {
    mockMvc
        .perform(
            post("/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("not-an-email", "a-strong-passphrase")))
        .andExpect(status().isBadRequest());
  }
}
