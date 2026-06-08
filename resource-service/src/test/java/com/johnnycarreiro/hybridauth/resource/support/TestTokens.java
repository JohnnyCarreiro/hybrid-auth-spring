package com.johnnycarreiro.hybridauth.resource.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Mints access tokens shaped exactly like the auth-service's ({@code sub}, {@code email}, {@code
 * email_verified}, {@code jti}, {@code iat}, {@code exp}, RS256 with a {@code kid}), signed with
 * the test RSA key {@link TestSecurityConfig#TEST_KEY}. The integration tests use these to
 * authenticate MockMvc requests without standing up a real auth-service / JWKS endpoint.
 */
public class TestTokens {

  private final RSAKey signingKey;

  public TestTokens(RSAKey signingKey) {
    this.signingKey = signingKey;
  }

  /** A signed, currently-valid access token for {@code sub} with the given identity claims. */
  public String mint(UUID sub, String email, boolean emailVerified) {
    return sign(sub, email, emailVerified, Instant.now().plusSeconds(900));
  }

  /** An already-expired token (for the 401 path). */
  public String mintExpired(UUID sub, String email) {
    return sign(sub, email, true, Instant.now().minusSeconds(60));
  }

  /** Convenience: the full {@code Authorization} header value for a valid token. */
  public String bearer(UUID sub, String email) {
    return "Bearer " + mint(sub, email, true);
  }

  private String sign(UUID sub, String email, boolean emailVerified, Instant expiry) {
    try {
      JWTClaimsSet claims =
          new JWTClaimsSet.Builder()
              .subject(sub.toString())
              .claim("email", email)
              .claim("email_verified", emailVerified)
              .issueTime(Date.from(Instant.now()))
              .expirationTime(Date.from(expiry))
              .jwtID(UUID.randomUUID().toString())
              .build();
      SignedJWT jwt =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
              claims);
      jwt.sign(new RSASSASigner(signingKey));
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException("failed to mint test token", e);
    }
  }
}
