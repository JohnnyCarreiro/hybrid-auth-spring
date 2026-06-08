package com.johnnycarreiro.hybridauth.auth.support.error;

/**
 * Base type for every expected, domain-level auth failure.
 *
 * <p>Concrete subclasses live next to the rule they defend (e.g. {@code EmailAlreadyTakenException}
 * in the identity package). Each carries an {@link AuthErrorCode} so the web edge can map it to a
 * response without a chain of {@code instanceof} checks, and a human-readable message stating the
 * offending value and/or the expected shape (playbook §10). These are never swallowed and never
 * caught as a bare {@code Exception}.
 */
public abstract class AuthException extends RuntimeException {

  private final transient AuthErrorCode code;

  protected AuthException(AuthErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  /** The stable code identifying this failure. */
  public AuthErrorCode code() {
    return code;
  }
}
