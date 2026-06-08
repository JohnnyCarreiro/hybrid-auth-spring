package com.johnnycarreiro.hybridauth.resource.domain.task;

import com.johnnycarreiro.hybridauth.resource.domain.shared.ResourceErrorCode;
import com.johnnycarreiro.hybridauth.resource.domain.shared.ResourceException;
import java.util.UUID;

/**
 * No task with this id is visible to the caller (SDD-002 §5 → HTTP 404).
 *
 * <p>Raised when the id does not exist <em>or</em> when the task's parent project is owned by
 * someone else — indistinguishable, so cross-user probing learns nothing.
 */
public class TaskNotFoundException extends ResourceException {

  public TaskNotFoundException(UUID id) {
    super(ResourceErrorCode.TASK_NOT_FOUND, "task not found: " + id);
  }
}
