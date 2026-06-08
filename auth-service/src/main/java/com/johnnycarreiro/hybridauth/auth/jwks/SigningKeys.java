package com.johnnycarreiro.hybridauth.auth.jwks;

import com.johnnycarreiro.hybridauth.auth.support.IdMint;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The signing-key lifecycle (SDD-001 §2/§4): generation, the active key, rotation, and the
 * published public set. This is the only place RSA key material is turned into a Nimbus {@code
 * RSAKey}; the {@link SigningKey} aggregate stays a plain string holder.
 *
 * <p><b>Cadence is pinned in code, not env-overridable</b> (ADR-0002 / SDD-001 §4 invariant 5). A
 * key is active for {@link #ACTIVE_PERIOD}; on rotation it stays servable for {@link #GRACE} more
 * so that access tokens signed just before the cut keep verifying against the JWKS. Putting these
 * behind properties would let an operator silently weaken the rotation guarantee — so they don't
 * exist.
 *
 * <p>Private material is sealed by {@link PrivateKeyCipher} before it is persisted and decrypted
 * only here, in-process, to build the signing {@code RSAKey}; it never crosses the JWKS boundary
 * (SDD-001 §4 invariant 6). {@link #publicJwkSet()} returns public-only keys.
 */
@Service
public class SigningKeys {

  /** How long a freshly generated key stays the active signer before lazy rotation. */
  static final Duration ACTIVE_PERIOD = Duration.ofDays(90);

  /** How long a rotated-out key stays servable in the JWKS after it is retired. */
  static final Duration GRACE = Duration.ofDays(30);

  private static final int RSA_KEY_SIZE = 2048;

  private final SigningKeyRepository keys;
  private final PrivateKeyCipher cipher;

  public SigningKeys(SigningKeyRepository keys, PrivateKeyCipher cipher) {
    this.keys = keys;
    this.cipher = cipher;
  }

  /**
   * The active signing key as a Nimbus {@code RSAKey} <em>including private params</em> (so the
   * encoder can sign). Bootstraps the very first key on demand, and lazily rotates if the current
   * active key has outlived {@link #ACTIVE_PERIOD}. Never returns a key whose private half cannot
   * be recovered.
   */
  @Transactional
  public RSAKey activeSigningKey() {
    Instant now = Instant.now();
    SigningKey active = keys.findFirstByExpiresAtIsNull().orElse(null);
    if (active == null) {
      active = generateAndPersist(now);
    } else if (!active.createdAt().plus(ACTIVE_PERIOD).isAfter(now)) {
      // createdAt + ACTIVE_PERIOD <= now → past its active window: rotate before serving.
      active = rotate();
    }
    return toFullRsaKey(active);
  }

  /**
   * The published public key set: every servable key (active + in-grace) as a <em>public-only</em>
   * {@code RSAKey}. Private params are stripped here, so this can never leak signing material.
   */
  @Transactional(readOnly = true)
  public JWKSet publicJwkSet() {
    Instant now = Instant.now();
    List<RSAKey> publicKeys =
        keys.findByExpiresAtIsNullOrExpiresAtAfter(now).stream().map(this::toPublicRsaKey).toList();
    return new JWKSet(List.copyOf(publicKeys));
  }

  /**
   * Out-of-band rotation (the {@code rotateSigningKey} admin operation, SDD-001 §3): retire the
   * current active key into its grace window and mint a fresh active key, returning the new one. If
   * there is no active key yet this simply generates the first one. No HTTP endpoint is wired for
   * this at F2 — see the F2 report.
   */
  @Transactional
  public SigningKey rotate() {
    Instant now = Instant.now();
    keys.findFirstByExpiresAtIsNull()
        .ifPresent(
            current -> {
              current.retire(now.plus(GRACE));
              keys.save(current);
            });
    return generateAndPersist(now);
  }

  // --- internals -------------------------------------------------------------------------------

  private SigningKey generateAndPersist(Instant now) {
    UUID kid = IdMint.next();
    RSAKey rsaKey = generateRsaKey(kid);
    String publicJson = rsaKey.toPublicJWK().toJSONString();
    String sealedPrivate = cipher.encrypt(rsaKey.toJSONString());
    SigningKey key = SigningKey.create(kid, publicJson, sealedPrivate, now);
    return keys.save(key);
  }

  private RSAKey generateRsaKey(UUID kid) {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(RSA_KEY_SIZE);
      KeyPair pair = generator.generateKeyPair();
      return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
          .privateKey((RSAPrivateKey) pair.getPrivate())
          .keyID(kid.toString())
          .algorithm(JWSAlgorithm.RS256)
          .keyUse(KeyUse.SIGNATURE)
          .build();
    } catch (Exception e) {
      throw new IllegalStateException("failed to generate RSA signing key", e);
    }
  }

  private RSAKey toFullRsaKey(SigningKey key) {
    try {
      return RSAKey.parse(cipher.decrypt(key.privateKey()));
    } catch (ParseException e) {
      throw new IllegalStateException(
          "stored private JWK for kid=" + key.id() + " is malformed", e);
    }
  }

  private RSAKey toPublicRsaKey(SigningKey key) {
    try {
      return RSAKey.parse(key.publicKey());
    } catch (ParseException e) {
      throw new IllegalStateException("stored public JWK for kid=" + key.id() + " is malformed", e);
    }
  }
}
