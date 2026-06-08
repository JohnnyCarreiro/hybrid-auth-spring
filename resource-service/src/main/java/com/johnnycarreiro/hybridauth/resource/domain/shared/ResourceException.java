package com.johnnycarreiro.hybridauth.resource.domain.shared;

/**
 * Base type for every expected, domain-level failure of the resource domain.
 *
 * <p>Concrete subclasses live next to the rule they defend ({@code ProjectNotFoundException} in
 * {@code domain.project}, {@code TaskNotFoundException} in {@code domain.task}). Each carries a
 * {@link ResourceErrorCode} so the web edge maps it to a response without {@code instanceof}
 * chains, and a message stating the offending value (playbook §10). Never swallowed, never caught
 * as a bare {@code Exception}.
 */
public abstract class ResourceException extends RuntimeException {

  private final transient ResourceErrorCode code;

  protected ResourceException(ResourceErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  /** The stable code identifying this failure. */
  public ResourceErrorCode code() {
    return code;
  }
}
