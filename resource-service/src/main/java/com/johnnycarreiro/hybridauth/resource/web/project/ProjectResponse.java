package com.johnnycarreiro.hybridauth.resource.web.project;

import com.johnnycarreiro.hybridauth.resource.domain.project.Project;
import java.time.Instant;

/** Public projection of a {@link Project}. */
public record ProjectResponse(
    String id,
    String ownerId,
    String name,
    String description,
    Instant createdAt,
    Instant updatedAt) {

  public static ProjectResponse from(Project project) {
    return new ProjectResponse(
        project.id().toString(),
        project.ownerId().toString(),
        project.name(),
        project.description(),
        project.createdAt(),
        project.updatedAt());
  }
}
