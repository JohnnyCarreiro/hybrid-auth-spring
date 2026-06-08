package com.johnnycarreiro.hybridauth.auth.services;

import com.johnnycarreiro.hybridauth.auth.domain.session.InvalidRefreshException;
import com.johnnycarreiro.hybridauth.auth.domain.session.Session;
import com.johnnycarreiro.hybridauth.auth.domain.token.RefreshTokens;
import com.johnnycarreiro.hybridauth.auth.infra.database.SessionRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The sign-out use case (SDD-001 §3 {@code signOut}, §8 F6): revoke the presented session.
 *
 * <p>Sign-out is deliberately narrow — it revokes <em>only</em> the session behind the presented
 * refresh token, never the whole family (family-wide revocation is reserved for reuse-detection in
 * F5). After this, a rotate on that token fails with {@code SESSION_REVOKED}; the already-issued
 * access token is not invalidated and simply lives out its ≤15-minute TTL (the accepted gap,
 * SDD-001 §8 F6).
 *
 * <p>The lookup takes the same {@code SELECT … FOR UPDATE} write lock as rotation, so a sign-out
 * concurrent with a rotation of the same token serializes cleanly instead of racing. An unknown
 * token is indistinguishable from any other bad refresh — {@link InvalidRefreshException} (401).
 * Revocation is idempotent (see {@link Session#revoke}), so a repeated sign-out is harmless.
 */
@Service
public class SignOutService {

  private final SessionRepository sessions;
  private final RefreshTokens refreshTokens;

  public SignOutService(SessionRepository sessions, RefreshTokens refreshTokens) {
    this.sessions = sessions;
    this.refreshTokens = refreshTokens;
  }

  /**
   * Revoke the session behind {@code presentedRefreshToken}.
   *
   * @throws InvalidRefreshException no session matches the presented token
   */
  @Transactional
  public void signOut(String presentedRefreshToken) {
    String hash = refreshTokens.hash(presentedRefreshToken);
    Session session =
        sessions.findByTokenHashForUpdate(hash).orElseThrow(InvalidRefreshException::new);
    session.revoke(Instant.now());
    sessions.save(session);
  }
}
