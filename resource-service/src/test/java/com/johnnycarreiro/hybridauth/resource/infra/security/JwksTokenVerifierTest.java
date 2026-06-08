package com.johnnycarreiro.hybridauth.resource.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Direct test of the hand-built {@link JwksTokenVerifier} (ADR-0005) against a controllable in-JVM
 * JWKS endpoint (JDK {@code HttpServer}, no dependency). Proves the in-memory cache, the
 * refetch-once-on-rotation behavior, and that a forged or expired token is rejected (→ 401) after a
 * single refetch.
 */
class JwksTokenVerifierTest {

  private static HttpServer server;
  private static String jwksUri;
  private static final AtomicReference<String> servedJwks = new AtomicReference<>("{\"keys\":[]}");
  private static final AtomicInteger fetchCount = new AtomicInteger();

  @BeforeAll
  static void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/.well-known/jwks.json",
        exchange -> {
          fetchCount.incrementAndGet();
          byte[] body = servedJwks.get().getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
          }
        });
    server.start();
    jwksUri = "http://127.0.0.1:" + server.getAddress().getPort() + "/.well-known/jwks.json";
  }

  @AfterAll
  static void stopServer() {
    server.stop(0);
  }

  @Test
  void verifiesATokenSignedByAServedKey() throws Exception {
    RSAKey key = rsaKey("k1");
    serve(key);
    JwksTokenVerifier verifier = new JwksTokenVerifier(jwksUri);

    UUID sub = UUID.randomUUID();
    Jwt jwt = verifier.decode(mint(key, sub, future()));

    assertThat(jwt.getSubject()).isEqualTo(sub.toString());
  }

  @Test
  void cachesKeysAndDoesNotRefetchOnTheHappyPath() throws Exception {
    RSAKey key = rsaKey("k1");
    serve(key);
    JwksTokenVerifier verifier = new JwksTokenVerifier(jwksUri);

    int before = fetchCount.get();
    verifier.decode(mint(key, UUID.randomUUID(), future())); // first call loads
    verifier.decode(mint(key, UUID.randomUUID(), future())); // second call hits the cache
    verifier.decode(mint(key, UUID.randomUUID(), future()));

    assertThat(fetchCount.get() - before).isEqualTo(1); // exactly one fetch for three verifies
  }

  @Test
  void refetchesOnceWhenTheSigningKeyHasRotated() throws Exception {
    RSAKey oldKey = rsaKey("k1");
    serve(oldKey);
    JwksTokenVerifier verifier = new JwksTokenVerifier(jwksUri);
    verifier.decode(mint(oldKey, UUID.randomUUID(), future())); // cache k1

    RSAKey newKey = rsaKey("k2"); // auth rotated: only k2 is served now
    serve(newKey);
    UUID sub = UUID.randomUUID();

    Jwt jwt =
        verifier.decode(mint(newKey, sub, future())); // cached k1 misses → refetch → k2 verifies

    assertThat(jwt.getSubject()).isEqualTo(sub.toString());
  }

  @Test
  void rejectsAForgedSignature() throws Exception {
    RSAKey realKey = rsaKey("k1");
    serve(realKey);
    RSAKey rogueKey = rsaKey("k1"); // same kid, different key material
    JwksTokenVerifier verifier = new JwksTokenVerifier(jwksUri);

    assertThatThrownBy(() -> verifier.decode(mint(rogueKey, UUID.randomUUID(), future())))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void rejectsAnExpiredToken() throws Exception {
    RSAKey key = rsaKey("k1");
    serve(key);
    JwksTokenVerifier verifier = new JwksTokenVerifier(jwksUri);

    assertThatThrownBy(() -> verifier.decode(mint(key, UUID.randomUUID(), past())))
        .isInstanceOf(JwtException.class);
  }

  // --- helpers ---

  private static RSAKey rsaKey(String kid) throws JOSEException {
    return new RSAKeyGenerator(2048).keyID(kid).generate();
  }

  private static void serve(RSAKey... keys) {
    List<com.nimbusds.jose.jwk.JWK> publicKeys =
        Arrays.stream(keys).map(k -> (com.nimbusds.jose.jwk.JWK) k.toPublicJWK()).toList();
    servedJwks.set(new JWKSet(publicKeys).toString()); // public-only JSON
  }

  private static String mint(RSAKey signingKey, UUID sub, Instant expiry) throws JOSEException {
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(sub.toString())
            .issueTime(Date.from(Instant.now().minusSeconds(5)))
            .expirationTime(Date.from(expiry))
            .jwtID(UUID.randomUUID().toString())
            .build();
    SignedJWT jwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(), claims);
    jwt.sign(new RSASSASigner(signingKey));
    return jwt.serialize();
  }

  private static Instant future() {
    return Instant.now().plusSeconds(900);
  }

  private static Instant past() {
    return Instant.now().minusSeconds(60);
  }
}
