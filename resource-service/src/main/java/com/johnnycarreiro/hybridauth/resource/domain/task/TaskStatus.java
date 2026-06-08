package com.johnnycarreiro.hybridauth.resource.domain.task;

/**
 * The lifecycle state of a {@link Task} (SDD-002 §2). Persisted as its name via
 * {@code @Enumerated(STRING)} into the {@code tasks.status} text column (default {@code TODO}).
 */
public enum TaskStatus {
  TODO,
  DOING,
  DONE
}
