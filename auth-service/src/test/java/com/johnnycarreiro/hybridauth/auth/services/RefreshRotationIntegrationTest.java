package com.johnnycarreiro.hybridauth.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnnycarreiro.hybridauth.auth.domain.session.ReuseDetectedException;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * F5 acceptance (SDD-001 §8 F5) — refresh rotation + reuse-detection, the centerpiece:
 *
 * <ol>
 *   <li>a valid refresh rotates → 200 new pair, old session {@code rotated_at} set, a chained child
 *       (same family, {@code parent_id}, sliding {@code expires_at}); the new access JWT verifies
 *       against the served JWKS;
 *   <li>replaying a rotated token → 401 {@code REFRESH_REUSE_DETECTED} and the <b>whole family is
 *       revoked</b>; the child is then dead too ({@code SESSION_REVOKED});
 *   <li>two concurrent rotations of the same token race through the {@code SELECT … FOR UPDATE}
 *       lock → exactly one wins, the other is reuse, family revoked;
 *   <li>a revoked session → 401 {@code SESSION_REVOKED};
 *   <li>an expired session → 401 {@code SESSION_EXPIRED}.
 * </ol>
 */
class RefreshRotationIntegrationTest extends AbstractAuthIT {

  private static final String EMAIL = "linus@example.com";
  private static final String PASSWORD = "a-strong-passphrase";

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository users;
  @Autowired SessionRepository sessions;
  @Autowired RefreshTokens refreshTokens;
  @Autowired RotateTokenService rotateTokenService;
  @Autowired TransactionTemplate tx;

  private final ObjectMapper json = new ObjectMapper();

  @BeforeEach
  void clean() {
    sessions.deleteAll();
    users.deleteAll();
  }

  // --- helpers ---------------------------------------------------------------

  private void signUp() throws Exception {
    mockMvc
        .perform(
            post("/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"%s\",\"password\":\"%s\",\"name\":\"Linus\"}"
                        .formatted(EMAIL, PASSWORD)))
        .andExpect(status().isOk());
  }

  /** Sign up + sign in against the real endpoints, returning the raw refresh token. */
  private String signUpAndSignIn() throws Exception {
    signUp();
    MvcResult signIn =
        mockMvc
            .perform(
                post("/auth/sign-in")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();
    return json.readTree(signIn.getResponse().getContentAsString()).get("refreshToken").asText();
  }

  private MvcResult rotate(String refreshToken) throws Exception {
    return mockMvc
        .perform(
            post("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
        .andReturn();
  }

  private Session sessionByRawToken(String rawToken) {
    String hash = refreshTokens.hash(rawToken);
    return sessions.findAll().stream()
        .filter(s -> s.tokenHash().equals(hash))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no session for the presented token"));
  }

  /** Build a decoder from the served public JWKS, exactly as the resource-service will. */
  private JwtDecoder jwksDecoder() throws Exception {
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
    return new NimbusJwtDecoder(processor);
  }

  // --- 1. happy rotation -----------------------------------------------------

  @Test
  void valid_refresh_rotates_to_a_new_pair_chains_a_child_and_stamps_the_parent() throws Exception {
    String oldRefresh = signUpAndSignIn();
    Session oldSession = sessionByRawToken(oldRefresh);
    assertThat(oldSession.rotatedAt()).isNull();

    MvcResult result = rotate(oldRefresh);
    assertThat(result.getResponse().getStatus()).isEqualTo(200);

    JsonNode body = json.readTree(result.getResponse().getContentAsString());
    String newAccess = body.get("accessToken").asText();
    String newRefresh = body.get("refreshToken").asText();
    assertThat(newAccess).isNotBlank();
    assertThat(newRefresh).isNotBlank().isNotEqualTo(oldRefresh);

    // The old session is now rotated.
    Session oldAfter = sessions.findById(oldSession.id()).orElseThrow();
    assertThat(oldAfter.rotatedAt()).isNotNull();
    assertThat(oldAfter.revokedAt()).isNull();

    // A new child session is chained: parent_id = old.id, same family, fresh window from now.
    Session child = sessionByRawToken(newRefresh);
    assertThat(child.id()).isNotEqualTo(oldSession.id());
    assertThat(child.parentId()).isEqualTo(oldSession.id());
    assertThat(child.familyId()).isEqualTo(oldSession.familyId());
    assertThat(child.rotatedAt()).isNull();
    assertThat(child.revokedAt()).isNull();

    // Sliding window: child window length ≈ parent's original (7 days), measured from
    // child.createdAt.
    Duration parentWindow = Duration.between(oldSession.createdAt(), oldSession.expiresAt());
    Duration childWindow = Duration.between(child.createdAt(), child.expiresAt());
    assertThat(childWindow.minus(parentWindow).abs()).isLessThan(Duration.ofSeconds(2));
    assertThat(parentWindow).isCloseTo(Duration.ofDays(7), Duration.ofSeconds(5));

    // The new access token verifies against the served JWKS.
    Jwt verified = jwksDecoder().decode(newAccess);
    assertThat(verified.getSubject()).isEqualTo(oldSession.userId().toString());
  }

  // --- 2. reuse after rotate ⇒ family revoked --------------------------------

  @Test
  void replaying_a_rotated_token_revokes_the_whole_family_and_kills_the_child() throws Exception {
    String oldRefresh = signUpAndSignIn();
    Session oldSession = sessionByRawToken(oldRefresh);
    UUID familyId = oldSession.familyId();

    // First rotation succeeds (old → child).
    MvcResult first = rotate(oldRefresh);
    assertThat(first.getResponse().getStatus()).isEqualTo(200);
    String childRefresh =
        json.readTree(first.getResponse().getContentAsString()).get("refreshToken").asText();

    // Replaying the OLD (now rotated) token → 401 REFRESH_REUSE_DETECTED.
    mockMvc
        .perform(
            post("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(oldRefresh)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("REFRESH_REUSE_DETECTED"));

    // EVERY session in the family now has revoked_at set (old + child).
    List<Session> family =
        sessions.findAll().stream().filter(s -> s.familyId().equals(familyId)).toList();
    assertThat(family).hasSize(2);
    assertThat(family).allSatisfy(s -> assertThat(s.revokedAt()).isNotNull());

    // The child is dead too: rotating it → 401 SESSION_REVOKED (the family is revoked, not
    // rotated).
    mockMvc
        .perform(
            post("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(childRefresh)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("SESSION_REVOKED"));
  }

  // --- 3. concurrency / the FOR-UPDATE race ----------------------------------

  @Test
  void two_concurrent_rotations_of_the_same_token_yield_one_winner_and_one_reuse()
      throws Exception {
    String refresh = signUpAndSignIn();
    UUID familyId = sessionByRawToken(refresh).familyId();

    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch release = new CountDownLatch(1);

    Callable<Outcome> task =
        () -> {
          release.await(); // both threads block here, then fire together
          try {
            rotateTokenService.rotate(refresh);
            return Outcome.WON;
          } catch (ReuseDetectedException e) {
            return Outcome.REUSE;
          }
        };

    Future<Outcome> a = pool.submit(task);
    Future<Outcome> b = pool.submit(task);
    release.countDown(); // release them together

    Outcome ra = a.get(30, TimeUnit.SECONDS);
    Outcome rb = b.get(30, TimeUnit.SECONDS);
    pool.shutdownNow();

    // Exactly one won; the other saw rotatedAt set under the lock and was reuse.
    assertThat(List.of(ra, rb)).containsExactlyInAnyOrder(Outcome.WON, Outcome.REUSE);

    // Reuse killed the family: every still-existing family member is revoked.
    List<Session> family =
        sessions.findAll().stream().filter(s -> s.familyId().equals(familyId)).toList();
    assertThat(family).isNotEmpty();
    assertThat(family).allSatisfy(s -> assertThat(s.revokedAt()).isNotNull());
  }

  private enum Outcome {
    WON,
    REUSE
  }

  // --- 4. revoked session → 401 SESSION_REVOKED ------------------------------

  @Test
  void rotating_a_revoked_session_returns_401_session_revoked() throws Exception {
    String refresh = signUpAndSignIn();
    Session session = sessionByRawToken(refresh);

    // Revoke directly via the family-revoke repo call (F6's sign-out path isn't built yet). The
    // @Modifying bulk update needs an active transaction, so run it inside a TransactionTemplate.
    int revoked = tx.execute(status -> sessions.revokeFamily(session.familyId(), Instant.now()));
    assertThat(revoked).isEqualTo(1);

    mockMvc
        .perform(
            post("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refresh)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("SESSION_REVOKED"));
  }

  // --- 5. expired session → 401 SESSION_EXPIRED ------------------------------

  @Test
  void rotating_an_expired_session_returns_401_session_expired() throws Exception {
    signUp();
    UUID userId = users.findByEmail_Value(EMAIL).orElseThrow().id();

    // Persist a session whose window already closed (expires_at in the past).
    String raw = refreshTokens.generate();
    Session expired =
        Session.openRoot(
            userId,
            refreshTokens.hash(raw),
            Instant.now().minus(Duration.ofMinutes(1)),
            "127.0.0.1",
            "test");
    sessions.saveAndFlush(expired);

    mockMvc
        .perform(
            post("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(raw)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("SESSION_EXPIRED"));
  }

  // --- 6. unknown token → 401 INVALID_REFRESH (negative case) ----------------

  @Test
  void rotating_an_unknown_token_returns_401_invalid_refresh() throws Exception {
    mockMvc
        .perform(
            post("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshTokens.generate())))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_REFRESH"));
  }
}
