package com.johnnycarreiro.hybridauth.resource.domain.project;

import com.johnnycarreiro.hybridauth.resource.domain.shared.ResourceErrorCode;
import com.johnnycarreiro.hybridauth.resource.domain.shared.ResourceException;
import java.util.UUID;

/**
 * No project with this id is visible to the caller (SDD-002 §5 → HTTP 404).
 *
 * <p>Raised both when the id does not exist and when it exists but is owned by someone else —
 * deliberately indistinguishable, so the API never confirms another user's resource (the
 * 404-over-403 ownership convention).
 */
public class ProjectNotFoundException extends ResourceException {

  public ProjectNotFoundException(UUID id) {
    super(ResourceErrorCode.PROJECT_NOT_FOUND, "project not found: " + id);
  }
}
