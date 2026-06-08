package com.johnnycarreiro.hybridauth.auth.sessions;

import com.johnnycarreiro.hybridauth.auth.identity.User;
import com.johnnycarreiro.hybridauth.auth.jwks.SigningKeys;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Mint short-lived RS256 access tokens (SDD-001 §3 {@code mintAccessToken}).
 *
 * <p>This is the issuer half of the hybrid scheme: a stateless JWT the resource-service verifies
 * locally against the JWKS, with no callback to auth. It is signed with the current active signing
 * key (private params held only in-process) and stamped with that key's {@code kid} in the JWS
 * header, so a verifier can resolve the right public key from {@code /.well-known/jwks.json}
 * (SDD-001 §4 invariant 8).
 *
 * <p><b>Lifetime is pinned in code</b> (ADR-0002 / SDD-001 §4 invariant 5): {@link #ACCESS_TTL} is
 * not env-overridable — exposing it as a property would let an operator silently widen the blast
 * radius of a leaked token, so it doesn't exist. Every token gets a fresh {@code jti} (invariant
 * 8).
 */
@Component
public class AccessTokens {

  /** Access-token lifetime — pinned, not env-overridable (SDD-001 §4 invariant 5). */
  static final Duration ACCESS_TTL = Duration.ofMinutes(15);

  private final JwtEncoder jwtEncoder;
  private final SigningKeys signingKeys;

  public AccessTokens(JwtEncoder jwtEncoder, SigningKeys signingKeys) {
    this.jwtEncoder = jwtEncoder;
    this.signingKeys = signingKeys;
  }

  /**
   * Mint a signed RS256 access JWT for {@code user}: {@code sub} = user id, {@code email} and
   * {@code email_verified} claims, a fresh random {@code jti}, and a 15-minute window. The JWS
   * header carries the active key's {@code kid}.
   */
  public String mint(User user) {
    Instant now = Instant.now();
    String kid = signingKeys.activeSigningKey().getKeyID();

    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(user.id().toString())
            .claim("email", user.email().value())
            .claim("email_verified", user.emailVerified())
            .issuedAt(now)
            .expiresAt(now.plus(ACCESS_TTL))
            .id(UUID.randomUUID().toString())
            .build();

    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(kid).build();

    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
