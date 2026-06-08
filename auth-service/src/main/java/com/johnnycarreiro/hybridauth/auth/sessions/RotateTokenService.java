package com.johnnycarreiro.hybridauth.auth.sessions;

import com.johnnycarreiro.hybridauth.auth.identity.User;
import com.johnnycarreiro.hybridauth.auth.identity.UserRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The refresh-token rotation use case (SDD-001 §3 {@code rotateToken}, §8 F5) — the centerpiece of
 * the scheme. Exchanges a presented opaque refresh token for a fresh hybrid pair (new access JWT +
 * new refresh), rotating the session chain and detecting token reuse.
 *
 * <p><b>Algorithm (one {@code @Transactional}).</b> Hash the presented token and look up its
 * session <em>under a pessimistic write lock</em> ({@code SELECT … FOR UPDATE}). Then, in order:
 *
 * <ol>
 *   <li>no row → {@link InvalidRefreshException} (the token was never issued / is forged);
 *   <li>{@code rotatedAt} set → <b>reuse</b>: revoke the whole family and throw {@link
 *       ReuseDetectedException} (SDD-001 §4 invariant 2 — the stolen-refresh defense);
 *   <li>{@code revokedAt} set → {@link SessionRevokedException} (signed out, or family already
 *       killed);
 *   <li>expired → {@link SessionExpiredException};
 *   <li>otherwise rotate: mint a new opaque refresh, chain a child session (sliding window,
 *       invariant 4), stamp the parent {@code rotatedAt}, mint a new access JWT, return both.
 * </ol>
 *
 * <p><b>Concurrency (invariant 3).</b> Two concurrent rotations of the same token serialize on the
 * row lock: the first commits its {@code rotatedAt}; the second acquires the lock, re-reads, sees
 * {@code rotatedAt} set, and is treated as reuse — exactly one caller wins, the other kills the
 * family.
 *
 * <p><b>Why the revoke survives the thrown 401.</b> The family revoke must commit even though we
 * are about to throw {@link ReuseDetectedException} to signal the 401. A thrown {@code
 * RuntimeException} normally marks the transaction rollback-only, which would undo the revoke. We
 * cannot revoke in a separate {@code REQUIRES_NEW} transaction either: this method holds a {@code
 * PESSIMISTIC_WRITE} row lock on the presented session (the {@code SELECT … FOR UPDATE}), and that
 * row is part of the very family the revoke {@code UPDATE} targets — a nested transaction would
 * block forever waiting for a lock its own outer transaction holds (a self-deadlock Postgres cannot
 * break). So the revoke runs <em>inline, in this same transaction</em> (one lock, no contention)
 * and the throw is declared {@code noRollbackFor = ReuseDetectedException.class}: the transaction
 * <strong>commits</strong> — persisting the family revocation — and the exception still propagates
 * out to become the 401.
 */
@Service
public class RotateTokenService {

  private static final Logger log = LoggerFactory.getLogger(RotateTokenService.class);

  private final SessionRepository sessions;
  private final RefreshTokens refreshTokens;
  private final AccessTokens accessTokens;
  private final UserRepository users;

  public RotateTokenService(
      SessionRepository sessions,
      RefreshTokens refreshTokens,
      AccessTokens accessTokens,
      UserRepository users) {
    this.sessions = sessions;
    this.refreshTokens = refreshTokens;
    this.accessTokens = accessTokens;
    this.users = users;
  }

  /**
   * Rotate {@code presentedRefreshToken} into a fresh pair, or reject (401 family) on any of the
   * four failure conditions above.
   *
   * @throws InvalidRefreshException no session matches the presented token
   * @throws ReuseDetectedException the token was already rotated — family revoked
   * @throws SessionRevokedException the session was revoked (sign-out / family death)
   * @throws SessionExpiredException the session's window has closed
   */
  @Transactional(noRollbackFor = ReuseDetectedException.class)
  public RotatedTokens rotate(String presentedRefreshToken) {
    String hash = refreshTokens.hash(presentedRefreshToken);
    Session session =
        sessions.findByTokenHashForUpdate(hash).orElseThrow(InvalidRefreshException::new);

    Instant now = Instant.now();

    if (session.rotatedAt() != null) {
      // Reuse: a spent token was replayed. Scorch the family inline (we already hold its row lock),
      // log a security event, then reject. noRollbackFor lets this transaction COMMIT the revoke
      // even though we throw to produce the 401 — see the class doc-comment for why not
      // REQUIRES_NEW.
      int revoked = sessions.revokeFamily(session.familyId(), now);
      log.warn(
          "refresh-token reuse detected; revoked {} session(s) in family familyId={} (presented session id={})",
          revoked,
          session.familyId(),
          session.id());
      throw new ReuseDetectedException();
    }
    if (session.revokedAt() != null) {
      throw new SessionRevokedException();
    }
    if (session.isExpired(now)) {
      throw new SessionExpiredException();
    }

    String newRaw = refreshTokens.generate();
    Session child = Session.rotateChild(session, refreshTokens.hash(newRaw), now);
    sessions.save(child);

    session.rotate(now);
    sessions.save(session);

    User user =
        users
            .findById(session.userId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "session references a missing user: " + session.userId()));
    String accessToken = accessTokens.mint(user);

    return new RotatedTokens(accessToken, newRaw);
  }

  /**
   * The output of a successful rotation: a freshly minted access JWT and the new <em>raw</em>
   * opaque refresh token (returned once, never persisted in clear — SDD-001 §4 invariant 7).
   */
  public record RotatedTokens(String accessToken, String refreshToken) {}
}
