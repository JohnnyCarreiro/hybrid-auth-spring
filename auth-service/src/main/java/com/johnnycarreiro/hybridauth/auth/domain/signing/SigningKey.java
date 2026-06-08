package com.johnnycarreiro.hybridauth.auth.domain.signing;

import com.johnnycarreiro.hybridauth.auth.domain.shared.IdMint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The signing-key aggregate (SDD-001 §2): one RS256 key pair in the issuer's rotating key set.
 *
 * <p>Like every aggregate in this domain the key stamps its own identity via {@link IdMint} rather
 * than delegating to the ORM — and here the id has a second meaning: it <strong>is</strong> the JWK
 * {@code kid} published in the JWKS and stamped into every access token's JWS header (SDD-001 §4
 * invariant 8). The aggregate is born consistent through {@link #create}; JPA gets only a protected
 * no-arg constructor.
 *
 * <p>The class deliberately holds only serialized strings and a lifecycle rule — the public key as
 * JWK JSON and the private key as an AES-GCM ciphertext blob. It imports <em>no</em> Nimbus or
 * Spring type: turning bytes into a {@code RSAKey}, signing, and encryption all live in the {@code
 * jwks} service layer, keeping the domain infra-free. The private key is never stored or returned
 * in clear (SDD-001 §4 invariant 6).
 *
 * <p>Lifecycle, expressed by {@code expiresAt}:
 *
 * <ul>
 *   <li><b>NULL</b> — the single active key; the one the encoder signs with.
 *   <li><b>future</b> — rotated out, still <em>servable</em> in the JWKS during the grace window so
 *       tokens signed just before rotation keep verifying.
 *   <li><b>past</b> — beyond grace; no longer served and safe to prune.
 * </ul>
 */
@Entity
@Table(name = "jwks")
public class SigningKey {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "public_key", nullable = false)
  private String publicKey;

  @Column(name = "private_key", nullable = false)
  private String privateKey;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "expires_at")
  private Instant expiresAt;

  protected SigningKey() {
    // JPA
  }

  private SigningKey(UUID id, String publicKey, String privateKey, Instant createdAt) {
    this.id = id;
    this.publicKey = publicKey;
    this.privateKey = privateKey;
    this.createdAt = createdAt;
    this.expiresAt = null;
  }

  /**
   * Mint a fresh, <em>active</em> signing key. The caller supplies the kid (so it can build the JWK
   * with the same id), the public key as JWK JSON, and the already-encrypted private-key blob; the
   * key is born active ({@code expiresAt == null}).
   */
  public static SigningKey create(
      UUID kid, String publicKeyJson, String encryptedPrivateKey, Instant createdAt) {
    Objects.requireNonNull(kid, "kid");
    Objects.requireNonNull(publicKeyJson, "publicKeyJson");
    Objects.requireNonNull(encryptedPrivateKey, "encryptedPrivateKey");
    Objects.requireNonNull(createdAt, "createdAt");
    return new SigningKey(kid, publicKeyJson, encryptedPrivateKey, createdAt);
  }

  /**
   * Whether this key should still appear in the published JWKS at {@code now} (active or in grace).
   */
  public boolean isServable(Instant now) {
    return expiresAt == null || expiresAt.isAfter(now);
  }

  /** Whether this is the active key (the one the encoder signs with). */
  public boolean isActive() {
    return expiresAt == null;
  }

  /**
   * Retire this key, opening its grace window: {@code expiresAt = graceUntil}. Only an active key
   * may be retired — retiring an already-retired key would silently move its grace deadline.
   */
  public void retire(Instant graceUntil) {
    Objects.requireNonNull(graceUntil, "graceUntil");
    if (!isActive()) {
      throw new IllegalStateException(
          "cannot retire a key that is already retired (kid="
              + id
              + ", expiresAt="
              + expiresAt
              + ")");
    }
    this.expiresAt = graceUntil;
  }

  public UUID id() {
    return id;
  }

  public String publicKey() {
    return publicKey;
  }

  public String privateKey() {
    return privateKey;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant expiresAt() {
    return expiresAt;
  }
}
