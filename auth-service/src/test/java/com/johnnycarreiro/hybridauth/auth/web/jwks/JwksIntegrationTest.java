package com.johnnycarreiro.hybridauth.auth.web.jwks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.johnnycarreiro.hybridauth.auth.domain.signing.PrivateKeyCipher;
import com.johnnycarreiro.hybridauth.auth.domain.signing.SigningKey;
import com.johnnycarreiro.hybridauth.auth.domain.signing.SigningKeys;
import com.johnnycarreiro.hybridauth.auth.infra.database.SigningKeyRepository;
import com.johnnycarreiro.hybridauth.auth.support.AbstractAuthIT;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * F2 acceptance (SDD-001 §8 F2): the JWKS serves public-only active+grace keys with a 600s cache
 * header; rotation adds a key and a token signed pre-rotation still verifies against the served set
 * during grace; private material is encrypted at rest.
 */
class JwksIntegrationTest extends AbstractAuthIT {

  @Autowired MockMvc mockMvc;
  @Autowired SigningKeys keys;
  @Autowired SigningKeyRepository repository;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired PrivateKeyCipher cipher;

  @BeforeEach
  void clean() {
    repository.deleteAll();
  }

  @Test
  void jwks_endpoint_serves_public_rsa_keys_with_cache_header_and_no_private_params()
      throws Exception {
    // Bootstrap the active key.
    keys.activeSigningKey();

    MvcResult result =
        mockMvc
            .perform(get("/.well-known/jwks.json"))
            .andExpect(status().isOk())
            .andExpect(
                header()
                    .string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=600")))
            .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
            .andExpect(jsonPath("$.keys[0].kid").exists())
            .andReturn();

    String body = result.getResponse().getContentAsString();
    // No private RSA params may ever appear in the published set.
    assertThat(body).doesNotContain("\"d\":").doesNotContain("\"p\":").doesNotContain("\"q\":");
  }

  @Test
  void rotation_adds_a_servable_key_and_a_pre_rotation_token_still_verifies_during_grace()
      throws Exception {
    // Mint a token signed by the active key, capturing its kid in the JWS header.
    UUID activeKid = UUID.fromString(keys.activeSigningKey().getKeyID());
    Jwt token =
        jwtEncoder.encode(
            JwtEncoderParameters.from(
                JwsHeader.with(() -> "RS256").keyId(activeKid.toString()).build(),
                JwtClaimsSet.builder()
                    .subject("test")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                    .build()));

    // Rotate: the old key enters grace, a fresh active key is minted.
    keys.rotate();

    // The served public set must now hold two servable keys (active + in-grace).
    assertThat(keys.publicJwkSet().getKeys()).hasSize(2);

    // Build a decoder from the *served* public JWKS and verify the pre-rotation token still passes
    // —
    // its signing key is in grace, so verification holds. The processor resolves the verification
    // key
    // by the token's kid against the served immutable set, exactly as the resource-service will.
    JWKSet served = keys.publicJwkSet();
    DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
    processor.setJWSKeySelector(
        new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, new ImmutableJWKSet<>(served)));
    JwtDecoder decoder = new NimbusJwtDecoder(processor);

    Jwt verified = decoder.decode(token.getTokenValue());
    assertThat(verified.getSubject()).isEqualTo("test");
    assertThat(verified.getHeaders().get("kid")).isEqualTo(activeKid.toString());
  }

  @Test
  void private_key_is_encrypted_at_rest_and_decrypts_to_a_jwk_with_private_params() {
    keys.activeSigningKey();

    SigningKey stored = repository.findFirstByExpiresAtIsNull().orElseThrow();

    // The stored column is ciphertext, not a parseable JWK.
    assertThat(stored.privateKey()).doesNotContain("\"kty\"").doesNotContain("\"d\"");

    // Decrypting it round-trips to the full private JWK JSON.
    String decrypted = cipher.decrypt(stored.privateKey());
    assertThat(decrypted).contains("\"kty\"").contains("\"d\"");
  }

  @Test
  void a_key_beyond_its_grace_window_is_not_served() {
    Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
    UUID kid = UUID.randomUUID();
    SigningKey expired =
        SigningKey.create(kid, "{\"kty\":\"RSA\"}", cipher.encrypt("{\"kty\":\"RSA\"}"), past);
    expired.retire(past); // grace deadline already in the past
    repository.save(expired);

    assertThat(keys.publicJwkSet().getKeys()).isEmpty();
  }
}
