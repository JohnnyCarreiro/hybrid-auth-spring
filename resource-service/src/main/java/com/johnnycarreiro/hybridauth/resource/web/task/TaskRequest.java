package com.johnnycarreiro.hybridauth.resource.web.task;

import com.johnnycarreiro.hybridauth.resource.domain.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound body for creating/updating a task. A null {@code status} defaults to {@link
 * TaskStatus#TODO} in the domain; an unrecognized status string fails Jackson enum binding and is
 * answered as a 400 by the exception handler.
 */
public record TaskRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    TaskStatus status) {}
