package com.johnnycarreiro.hybridauth.auth.identity;

import com.johnnycarreiro.hybridauth.auth.support.IdMint;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The identity aggregate: a registered account with an email and an Argon2id password hash.
 *
 * <p>The aggregate is responsible for everything that belongs to it — including its own identity.
 * It stamps a fresh UUID v7 at construction via {@link IdMint} instead of delegating id generation
 * to Hibernate, so an instance is fully valid in memory before it ever touches a store and a store
 * swap cannot change the id contract. There is no public constructor: {@link #register} is the only
 * way in, which keeps every {@code User} born in a consistent state.
 *
 * <p>The raw password never reaches this type — the caller hashes it first and passes the hash.
 */
@Entity
@Table(name = "users")
public class User {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Embedded private Email email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;

  @Column(name = "name")
  private String name;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected User() {
    // JPA
  }

  private User(UUID id, Email email, String passwordHash, String name, Instant now) {
    this.id = id;
    this.email = email;
    this.passwordHash = passwordHash;
    this.emailVerified = false;
    this.name = name;
    this.createdAt = now;
    this.updatedAt = now;
  }

  /**
   * Register a new account from a normalized email and an already-computed password hash. The id
   * and timestamps are assigned by the aggregate itself.
   */
  public static User register(Email email, String passwordHash, String name) {
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(passwordHash, "passwordHash");
    Instant now = Instant.now();
    return new User(IdMint.next(), email, passwordHash, name, now);
  }

  public UUID id() {
    return id;
  }

  public Email email() {
    return email;
  }

  public String passwordHash() {
    return passwordHash;
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
