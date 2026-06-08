package com.johnnycarreiro.hybridauth.resource.domain.task;

import com.johnnycarreiro.hybridauth.resource.domain.shared.IdMint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A task within a project (SDD-002 §2). It holds its parent's id ({@link #projectId}) but <b>no</b>
 * owner of its own — ownership is derived through the project, so a task can never disagree with
 * its project about who owns it (SDD-002 §4 invariant 2).
 *
 * <p>Born consistent via {@link #create} (fresh UUID v7 + timestamps, default {@link
 * TaskStatus#TODO}); JPA gets a {@code protected} no-arg constructor only.
 */
@Entity
@Table(name = "tasks")
public class Task {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "project_id", nullable = false, updatable = false)
  private UUID projectId;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "description")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private TaskStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Task() {
    // JPA
  }

  private Task(
      UUID id, UUID projectId, String title, String description, TaskStatus status, Instant now) {
    this.id = id;
    this.projectId = projectId;
    this.title = title;
    this.description = description;
    this.status = status;
    this.createdAt = now;
    this.updatedAt = now;
  }

  /**
   * Create a task under {@code projectId}; a null {@code status} defaults to {@link
   * TaskStatus#TODO}.
   */
  public static Task create(UUID projectId, String title, String description, TaskStatus status) {
    Objects.requireNonNull(projectId, "projectId");
    Objects.requireNonNull(title, "title");
    Instant now = Instant.now();
    return new Task(
        IdMint.next(),
        projectId,
        title,
        description,
        status == null ? TaskStatus.TODO : status,
        now);
  }

  /**
   * Apply an edit to the mutable fields and bump {@code updatedAt}. The parent project never
   * changes.
   */
  public void edit(String title, String description, TaskStatus status) {
    this.title = Objects.requireNonNull(title, "title");
    this.description = description;
    this.status = status == null ? TaskStatus.TODO : status;
    this.updatedAt = Instant.now();
  }

  public UUID id() {
    return id;
  }

  public UUID projectId() {
    return projectId;
  }

  public String title() {
    return title;
  }

  public String description() {
    return description;
  }

  public TaskStatus status() {
    return status;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }
}
