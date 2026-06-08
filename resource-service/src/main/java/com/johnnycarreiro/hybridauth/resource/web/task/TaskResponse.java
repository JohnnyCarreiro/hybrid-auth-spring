package com.johnnycarreiro.hybridauth.resource.web.task;

import com.johnnycarreiro.hybridauth.resource.domain.task.Task;
import java.time.Instant;

/** Public projection of a {@link Task}. */
public record TaskResponse(
    String id,
    String projectId,
    String title,
    String description,
    String status,
    Instant createdAt,
    Instant updatedAt) {

  public static TaskResponse from(Task task) {
    return new TaskResponse(
        task.id().toString(),
        task.projectId().toString(),
        task.title(),
        task.description(),
        task.status().name(),
        task.createdAt(),
        task.updatedAt());
  }
}
