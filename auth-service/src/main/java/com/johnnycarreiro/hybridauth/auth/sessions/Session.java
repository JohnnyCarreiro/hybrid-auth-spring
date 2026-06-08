package com.johnnycarreiro.hybridauth.auth.sessions;

import com.johnnycarreiro.hybridauth.auth.support.IdMint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The refresh-session aggregate (SDD-001 §2): one node in a refresh-token rotation chain.
 *
 * <p>A <strong>family</strong> — every session sharing a {@code familyId} — is the consistency
 * boundary for reuse-detection. A <em>root</em> session (born at sign-up/sign-in) opens a fresh
 * family: a new {@code familyId} and {@code parentId == null} (SDD-001 §4 invariant 1). When a
 * refresh is later exchanged (F5) the presented session is <em>rotated</em>: it gets a {@code
 * rotatedAt} stamp and a child session is chained in, inheriting the family's {@code familyId} and
 * pointing back via {@code parentId}. Presenting a refresh whose {@code rotatedAt} or {@code
 * revokedAt} is already set is reuse, and kills the whole family (invariant 2).
 *
 * <p><b>Lifecycle mutators, each next to the rule it enforces.</b> A login opens a root via {@link
 * #openRoot}; a refresh exchange chains a child with {@link #rotateChild} and spends the presented
 * session with {@link #rotate} (F5); sign-out ends a session with {@link #revoke} (F6). Family-wide
 * revocation on reuse is a bulk store operation, not an aggregate method (it spans many rows).
 *
 * <p>Like every aggregate in this domain it stamps its own UUID v7 identity via {@link IdMint}
 * rather than delegating to the ORM, is born consistent through a static factory ({@link
 * #openRoot}), and gives JPA only a protected no-arg constructor. It imports no Spring/Nimbus type
 * — the opaque token is hashed before it reaches this type, and only the SHA-256 {@code tokenHash}
 * is ever held (SDD-001 §4 invariant 7); the raw refresh token never enters the aggregate.
 */
@Entity
@Table(name = "sessions")
public class Session {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(name = "token_hash", nullable = false, unique = true)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "family_id", nullable = false, updatable = false)
  private UUID familyId;

  @Column(name = "parent_id", updatable = false)
  private UUID parentId;

  @Column(name = "rotated_at")
  private Instant rotatedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "ip_address", updatable = false)
  private String ipAddress;

  @Column(name = "user_agent", updatable = false)
  private String userAgent;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Session() {
    // JPA
  }

  private Session(
      UUID id,
      UUID userId,
      String tokenHash,
      Instant expiresAt,
      UUID familyId,
      String ipAddress,
      String userAgent,
      Instant now) {
    this.id = id;
    this.userId = userId;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
    this.familyId = familyId;
    this.parentId = null;
    this.rotatedAt = null;
    this.revokedAt = null;
    this.ipAddress = ipAddress;
    this.userAgent = userAgent;
    this.createdAt = now;
    this.updatedAt = now;
  }

  /**
   * Full constructor used by the rotation factory — a child inherits family and points to a parent.
   */
  private Session(
      UUID id,
      UUID userId,
      String tokenHash,
      Instant expiresAt,
      UUID familyId,
      UUID parentId,
      String ipAddress,
      String userAgent,
      Instant now) {
    this.id = id;
    this.userId = userId;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
    this.familyId = familyId;
    this.parentId = parentId;
    this.rotatedAt = null;
    this.revokedAt = null;
    this.ipAddress = ipAddress;
    this.userAgent = userAgent;
    this.createdAt = now;
    this.updatedAt = now;
  }

  /**
   * Open a fresh <em>root</em> session for a login (SDD-001 §4 invariant 1): a new {@code id} and a
   * new {@code familyId} (both UUID v7), {@code parentId == null}, no {@code rotatedAt}/{@code
   * revokedAt}, timestamps now. The caller supplies the already-hashed refresh token — the raw
   * token never enters the aggregate.
   */
  public static Session openRoot(
      UUID userId, String tokenHash, Instant expiresAt, String ipAddress, String userAgent) {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(tokenHash, "tokenHash");
    Objects.requireNonNull(expiresAt, "expiresAt");
    Instant now = Instant.now();
    return new Session(
        IdMint.next(), userId, tokenHash, expiresAt, IdMint.next(), ipAddress, userAgent, now);
  }

  /**
   * Chain a new <em>child</em> session onto a just-rotated {@code parent} (SDD-001 §4 invariants 1
   * + 4). The child gets a fresh UUID v7 {@code id} but inherits the parent's {@code userId} and
   * {@code familyId} (so reuse-detection still scopes the whole family) and points back via {@code
   * parentId == parent.id}. The caller supplies the new token's SHA-256 hash; the raw token never
   * enters the aggregate.
   *
   * <p><b>Sliding window (invariant 4).</b> {@code child.expiresAt = now + (parent.expiresAt −
   * parent.createdAt)} — the child gets a brand-new full-length window measured from <em>its</em>
   * creation, preserving the original window length without a remember-me flag. Audit context
   * ({@code ipAddress}/{@code userAgent}) is carried over from the parent.
   */
  public static Session rotateChild(Session parent, String tokenHash, Instant now) {
    Objects.requireNonNull(parent, "parent");
    Objects.requireNonNull(tokenHash, "tokenHash");
    Objects.requireNonNull(now, "now");
    Duration window = Duration.between(parent.createdAt, parent.expiresAt);
    return new Session(
        IdMint.next(),
        parent.userId,
        tokenHash,
        now.plus(window),
        parent.familyId,
        parent.id,
        parent.ipAddress,
        parent.userAgent,
        now);
  }

  /**
   * Stamp this session as <em>rotated</em> at {@code now} (SDD-001 §3 {@code rotateToken}): once a
   * refresh is exchanged, the presented session is spent and its token must never be valid again.
   * Setting {@code rotatedAt} is what turns a later replay of the same token into reuse — the
   * second presenter sees this stamp (committed under the pessimistic lock) and the family dies
   * (invariants 2–3). Idempotence is the caller's job: {@link #rotate} guards that the session is
   * still active, so a double-rotate is impossible on the happy path.
   *
   * @throws IllegalStateException if the session is no longer active (already rotated, revoked, or
   *     expired) — the use case must never reach here for a non-active session
   */
  public void rotate(Instant now) {
    Objects.requireNonNull(now, "now");
    if (!isActive(now)) {
      throw new IllegalStateException(
          "cannot rotate a session that is not active (id=%s, rotatedAt=%s, revokedAt=%s, expiresAt=%s)"
              .formatted(id, rotatedAt, revokedAt, expiresAt));
    }
    this.rotatedAt = now;
    this.updatedAt = now;
  }

  /**
   * Revoke this session at {@code now} (SDD-001 §3 {@code signOut}): a later rotate on it fails
   * with {@code SESSION_REVOKED}. Sign-out revokes only the <em>presented</em> session, never the
   * family (SDD-001 §4 — that family-wide kill is reserved for reuse-detection). Idempotent:
   * revoking an already-revoked session keeps the original {@code revokedAt}, so a double sign-out
   * is harmless.
   */
  public void revoke(Instant now) {
    Objects.requireNonNull(now, "now");
    if (revokedAt == null) {
      this.revokedAt = now;
      this.updatedAt = now;
    }
  }

  /** Whether this session's window has closed at {@code now}. */
  public boolean isExpired(Instant now) {
    return !expiresAt.isAfter(now);
  }

  /**
   * Whether this session can still be presented at {@code now}: not expired, rotated, or revoked.
   */
  public boolean isActive(Instant now) {
    return !isExpired(now) && rotatedAt == null && revokedAt == null;
  }

  public UUID id() {
    return id;
  }

  public UUID userId() {
    return userId;
  }

  public String tokenHash() {
    return tokenHash;
  }

  public Instant expiresAt() {
    return expiresAt;
  }

  public UUID familyId() {
    return familyId;
  }

  public UUID parentId() {
    return parentId;
  }

  public Instant rotatedAt() {
    return rotatedAt;
  }

  public Instant revokedAt() {
    return revokedAt;
  }

  public String ipAddress() {
    return ipAddress;
  }

  public String userAgent() {
    return userAgent;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }
}
