package com.johnnycarreiro.hybridauth.auth.sessions;

import com.johnnycarreiro.hybridauth.auth.identity.Email;
import com.johnnycarreiro.hybridauth.auth.identity.User;
import com.johnnycarreiro.hybridauth.auth.identity.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The sign-in use case (SDD-001 §8 F3): verify credentials and issue hybrid credentials.
 *
 * <p>On success it mints two things at once — a short-lived RS256 access JWT (stateless, verified
 * against the JWKS) and an opaque refresh token backed by a fresh <em>root</em> {@link Session}
 * (server-side, the head of a new rotation family, SDD-001 §4 invariant 1). The raw refresh token
 * is returned to the caller once; only its SHA-256 hash is persisted (invariant 7).
 *
 * <p><b>No user enumeration.</b> An unknown email and a wrong password fail identically — a single
 * {@link InvalidCredentialsException} (401) — and an Argon2 verify runs on both paths (against a
 * decoy hash when the email is unknown), so they are indistinguishable to the caller in result and
 * timing alike (SDD-001 §8 F3 validation). The constant-time compare itself lives in the Argon2id
 * {@link PasswordEncoder#matches}.
 *
 * <p>Lifetimes are pinned in code, not env-overridable (SDD-001 §4 invariant 5): refresh = {@link
 * #REFRESH_TTL}; the access TTL lives in {@link AccessTokens}.
 */
@Service
public class SignInService {

  /** Refresh-token lifetime — pinned, not env-overridable (SDD-001 §4 invariant 5). */
  static final Duration REFRESH_TTL = Duration.ofDays(7);

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final SessionRepository sessions;
  private final RefreshTokens refreshTokens;
  private final AccessTokens accessTokens;

  /**
   * A throwaway Argon2id hash computed once at startup. When the email is unknown there is no
   * stored hash to verify against, so we verify the supplied password against this decoy instead:
   * the unknown-email path then pays the same Argon2 cost as a wrong-password path, closing the
   * timing channel that would otherwise leak which emails are registered.
   */
  private final String decoyHash;

  public SignInService(
      UserRepository users,
      PasswordEncoder passwordEncoder,
      SessionRepository sessions,
      RefreshTokens refreshTokens,
      AccessTokens accessTokens) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.sessions = sessions;
    this.refreshTokens = refreshTokens;
    this.accessTokens = accessTokens;
    this.decoyHash = passwordEncoder.encode("invalid-credentials-decoy");
  }

  /**
   * Verify {@code rawEmail}/{@code rawPassword}; on success open a root session and return both
   * tokens plus the user. The {@code ip}/{@code userAgent} are captured on the session for audit.
   *
   * @throws InvalidCredentialsException if the email is unknown or the password does not match
   */
  @Transactional
  public SignInResult signIn(String rawEmail, String rawPassword, String ip, String userAgent) {
    Email email = Email.of(rawEmail);
    Optional<User> found = users.findByEmail_Value(email.value());

    // An absent account and a wrong password must be indistinguishable to the caller (no
    // enumeration), in both the error AND the timing: when the email is unknown we still run an
    // Argon2 verify — against the decoy hash — so the response time profile matches a real miss.
    User user = found.orElse(null);
    boolean matches;
    if (user != null) {
      matches = passwordEncoder.matches(rawPassword, user.passwordHash());
    } else {
      passwordEncoder.matches(rawPassword, decoyHash); // decoy verify — burn the same Argon2 cost
      matches = false;
    }
    if (!matches) {
      throw new InvalidCredentialsException();
    }

    Instant now = Instant.now();
    String rawRefresh = refreshTokens.generate();
    Session session =
        Session.openRoot(
            user.id(), refreshTokens.hash(rawRefresh), now.plus(REFRESH_TTL), ip, userAgent);
    sessions.save(session);

    String accessToken = accessTokens.mint(user);
    return new SignInResult(accessToken, rawRefresh, user);
  }

  /**
   * The output of a successful sign-in: a freshly minted access JWT, the <em>raw</em> opaque
   * refresh token (returned once, never persisted in clear), and the authenticated {@link User}.
   */
  public record SignInResult(String accessToken, String refreshToken, User user) {}
}
