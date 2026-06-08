package com.johnnycarreiro.hybridauth.resource.domain.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * The local <strong>mirror</strong> of an auth identity (ADR-0003 / ADR-0006 / SDD-002 §2).
 *
 * <p>This is a cache, not a source of truth: {@code auth.users} owns identity; the resource-service
 * keeps a copy of a few fields so the app domain can hold a real {@code projects.owner_id} foreign
 * key without reaching across the database boundary. Its {@link #id} is therefore <em>not</em>
 * minted here — it is the auth user id (the access-token {@code sub}), copied in verbatim (contrast
 * {@code IdMint}, which mints ids for {@code Project}/{@code Task}).
 *
 * <p><b>Create-only at this tier.</b> A row is provisioned on the first authenticated request that
 * presents a {@code sub} not yet seen ({@code AppUserRepository#provisionIfAbsent}); it is never
 * updated from the token afterwards. Propagating later changes (email/name) is deferred to an
 * auth-side event the resource-service would consume (ADR-0006). {@code ddl-auto: validate} —
 * Flyway owns the schema (V2).
 */
@Entity
@Table(name = "users")
public class AppUser {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;

  @Column(name = "name")
  private String name;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected AppUser() {
    // JPA
  }

  public UUID id() {
    return id;
  }

  public String email() {
    return email;
  }

  public boolean emailVerified() {
    return emailVerified;
  }

  public String name() {
    return name;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }
}
