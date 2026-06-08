package com.johnnycarreiro.hybridauth.resource.infra.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * The hand-built access-token verifier — the resource-service half of the hybrid scheme (ADR-0005 /
 * SDD-002 §3). A <strong>singleton</strong> {@link JwtDecoder} that holds the auth-service's public
 * signing keys <em>in memory</em> and verifies every Bearer JWT locally (RS256 signature + expiry),
 * with <b>no shared secret and no per-request call</b> to auth on the happy path.
 *
 * <p><b>Refetch-on-rotation, then 401.</b> Verification runs against the cached key set. On any
 * miss — the token's {@code kid} is absent (the auth-service rotated its signing key) <em>or</em>
 * the signature does not verify — the verifier refetches {@code /.well-known/jwks.json} exactly
 * <em>once</em>, replaces the cache, and retries. A second failure propagates as a {@link
 * JwtException}, which Spring Security's resource-server filter renders as a 401. So a freshly
 * rotated key is picked up automatically on the next request; a genuinely forged token costs one
 * (cacheable, cheap) JWKS fetch and is then rejected.
 *
 * <p>This is deliberately the explicit, owned version of what Spring's {@code jwk-set-uri} decoder
 * does internally (in-memory cache + refresh on unknown kid) — symmetric with the auth-service's
 * hand-built RS256 issuer, and the thing this reference exists to show (ADR-0005 refines ADR-0002).
 * It mirrors the auth-service's own {@code SecurityConfig#jwtDecoder} construction ({@link
 * DefaultJWTProcessor} + {@link JWSVerificationKeySelector}, RS256 only, default validators) — the
 * difference is the key source: there, the in-process public set; here, the remote JWKS, cached and
 * refetched.
 */
@Component
public class JwksTokenVerifier implements JwtDecoder {

  private static final Logger log = LoggerFactory.getLogger(JwksTokenVerifier.class);

  private static final int CONNECT_TIMEOUT_MS = 2_000;
  private static final int READ_TIMEOUT_MS = 2_000;
  private static final int SIZE_LIMIT_BYTES = 64 * 1024;

  private final URL jwksUri;

  /** The in-memory cache: a decoder closed over the last-fetched public key set. */
  private final AtomicReference<NimbusJwtDecoder> cached = new AtomicReference<>();

  public JwksTokenVerifier(@Value("${resource.auth.jwks-uri}") String jwksUri) {
    try {
      this.jwksUri = new URI(jwksUri).toURL();
    } catch (URISyntaxException | MalformedURLException | IllegalArgumentException badUri) {
      throw new IllegalStateException(
          "resource.auth.jwks-uri is not a valid URL: " + jwksUri, badUri);
    }
  }

  @Override
  public Jwt decode(String token) throws JwtException {
    NimbusJwtDecoder decoder = current();
    try {
      return decoder.decode(token);
    } catch (JwtException firstMiss) {
      // Unknown kid (rotation) or bad signature — the cache may be stale. Refetch once and retry.
      log.debug(
          "local JWKS verify failed ({}); refetching {} and retrying once",
          firstMiss.getMessage(),
          jwksUri);
      return refresh().decode(token); // a second failure propagates → 401
    }
  }

  /** The cached decoder, lazily loading the JWKS on the very first call. */
  private NimbusJwtDecoder current() {
    NimbusJwtDecoder decoder = cached.get();
    return decoder != null ? decoder : refresh();
  }

  /** Fetch the JWKS, rebuild the decoder over the fresh public set, and swap it into the cache. */
  private NimbusJwtDecoder refresh() {
    JWKSet keys;
    try {
      keys = JWKSet.load(jwksUri, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS, SIZE_LIMIT_BYTES);
    } catch (IOException | ParseException unreachable) {
      // The issuer is down or served garbage — we cannot prove the token, so reject (401), don't
      // 500.
      throw new JwtException("could not load JWKS from " + jwksUri, unreachable);
    }
    NimbusJwtDecoder decoder = decoderOver(keys);
    cached.set(decoder);
    return decoder;
  }

  /**
   * Build a NimbusJwtDecoder that verifies RS256 against a fixed public key set + default
   * validators.
   */
  private static NimbusJwtDecoder decoderOver(JWKSet keys) {
    JWKSource<SecurityContext> source = (selector, context) -> selector.select(keys);
    DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
    processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, source));
    NimbusJwtDecoder decoder = new NimbusJwtDecoder(processor);
    decoder.setJwtValidator(JwtValidators.createDefault()); // exp / nbf — rejects expired tokens
    return decoder;
  }
}
