package com.johnnycarreiro.hybridauth.resource.domain.project;

import com.johnnycarreiro.hybridauth.resource.domain.shared.IdMint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The top-level owned aggregate of the app domain (SDD-002 §2): a project belonging to exactly one
 * user.
 *
 * <p>Born consistent: no public constructor — {@link #create} is the only way in, stamping a fresh
 * UUID v7 ({@link IdMint}) and the timestamps. {@code ownerId} is the auth user id (the verified
 * token {@code sub}) and is immutable once set — a project never changes hands; it is the
 * authorization anchor (SDD-002 §4 invariant 1). JPA gets a {@code protected} no-arg constructor
 * only.
 */
@Entity
@Table(name = "projects")
public class Project {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "owner_id", nullable = false, updatable = false)
  private UUID ownerId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Project() {
    // JPA
  }

  private Project(UUID id, UUID ownerId, String name, String description, Instant now) {
    this.id = id;
    this.ownerId = ownerId;
    this.name = name;
    this.description = description;
    this.createdAt = now;
    this.updatedAt = now;
  }

  /**
   * Create a new project owned by {@code ownerId}; the aggregate assigns its own id + timestamps.
   */
  public static Project create(UUID ownerId, String name, String description) {
    Objects.requireNonNull(ownerId, "ownerId");
    Objects.requireNonNull(name, "name");
    Instant now = Instant.now();
    return new Project(IdMint.next(), ownerId, name, description, now);
  }

  /** Apply an edit to the mutable fields and bump {@code updatedAt}. Ownership never changes. */
  public void edit(String name, String description) {
    this.name = Objects.requireNonNull(name, "name");
    this.description = description;
    this.updatedAt = Instant.now();
  }

  public UUID id() {
    return id;
  }

  public UUID ownerId() {
    return ownerId;
  }

  public String name() {
    return name;
  }

  public String description() {
    return description;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }
}
