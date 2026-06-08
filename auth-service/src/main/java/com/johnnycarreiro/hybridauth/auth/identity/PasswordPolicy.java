package com.johnnycarreiro.hybridauth.auth.identity;

/**
 * The password policy enforced at registration.
 *
 * <p>Deliberately length-based rather than a composition rule (no "one upper, one digit, one
 * symbol"): length is the dominant factor in resistance to guessing, and composition rules push
 * users toward predictable patterns. The upper bound is a denial-of-service guard — Argon2id is
 * intentionally expensive, so an unbounded input is an attack surface. A failure raises {@link
 * WeakPasswordException} carrying the expected shape.
 */
public final class PasswordPolicy {

  public static final int MIN_LENGTH = 12;
  public static final int MAX_LENGTH = 200;

  private PasswordPolicy() {}

  /** Validate a raw password, throwing {@link WeakPasswordException} when it fails. */
  public static void check(String raw) {
    if (raw == null || raw.length() < MIN_LENGTH) {
      throw new WeakPasswordException("must be at least " + MIN_LENGTH + " characters");
    }
    if (raw.length() > MAX_LENGTH) {
      throw new WeakPasswordException("must be at most " + MAX_LENGTH + " characters");
    }
  }
}
