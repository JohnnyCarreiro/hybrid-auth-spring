package com.johnnycarreiro.hybridauth.auth.identity;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence port for the {@link User} aggregate. */
public interface UserRepository extends JpaRepository<User, UUID> {

  /**
   * Whether an account already exists for the given normalized email. The {@code Email_Value} path
   * traverses the embedded {@link Email} value object to its {@code value} column.
   */
  boolean existsByEmail_Value(String email);
}
