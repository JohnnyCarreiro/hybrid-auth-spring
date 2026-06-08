package com.johnnycarreiro.hybridauth.resource.domain.shared;

/**
 * The stable, internal error vocabulary of the resource (task/project) domain.
 *
 * <p>Mirrors the auth-service's {@code AuthErrorCode}: each variant carries its HTTP status as a
 * plain {@code int} so the domain stays free of any web type, and the single web edge ({@code
 * ResourceExceptionHandler}) is the only place a code becomes a response.
 *
 * <p><b>Ownership failures are 404, not 403</b> (SDD-002 §5): a request for a project/task the
 * caller does not own is answered as "not found" so the API never confirms the existence of another
 * user's resource. Authentication failures (missing/invalid/expired Bearer) are produced by Spring
 * Security's resource-server filter as a 401 and never reach this enum.
 */
public enum ResourceErrorCode {
  PROJECT_NOT_FOUND(404),
  TASK_NOT_FOUND(404);

  private final int httpStatus;

  ResourceErrorCode(int httpStatus) {
    this.httpStatus = httpStatus;
  }

  /** The HTTP status this code maps to at the API edge. */
  public int httpStatus() {
    return httpStatus;
  }
}
