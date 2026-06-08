package com.johnnycarreiro.hybridauth.auth.domain.shared;

/**
 * The stable, internal error vocabulary of the auth domain.
 *
 * <p>Each variant is a distinct code even when two share an HTTP status (several refresh-rotation
 * failures all surface as 401 but stay separable here — see SDD-001 §5). The {@code httpStatus} is
 * carried as a plain {@code int} on purpose: the domain layer stays free of any web framework type,
 * and the web edge ({@link AuthExceptionHandler}) is the only place that turns a code into a
 * response.
 */
public enum AuthErrorCode {
  EMAIL_ALREADY_TAKEN(409),
  WEAK_PASSWORD(422),
  INVALID_CREDENTIALS(401),
  UNAUTHENTICATED(401),
  INVALID_REFRESH(401),
  REFRESH_REUSE_DETECTED(401),
  SESSION_REVOKED(401),
  SESSION_EXPIRED(401);

  private final int httpStatus;

  AuthErrorCode(int httpStatus) {
    this.httpStatus = httpStatus;
  }

  /** The HTTP status this code maps to at the API edge. */
  public int httpStatus() {
    return httpStatus;
  }
}
