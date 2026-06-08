package com.johnnycarreiro.hybridauth.auth.identity;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The sign-up use case (SDD-001 §8 F1): register an account from an email + password.
 *
 * <p>Order matters — normalize the email and check the password policy <em>before</em> hashing, so
 * a rejected request never pays the Argon2id cost. The pre-check against {@link
 * UserRepository#existsByEmail_Value} gives a clean 409 on the common path; the unique index is the
 * real arbiter, so a lost race on concurrent sign-ups is caught by translating the resulting {@link
 * DataIntegrityViolationException} into the same {@link EmailAlreadyTakenException}. No auto-login
 * at MVP: this returns the persisted {@link User}, not a token.
 */
@Service
public class SignUpService {

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;

  public SignUpService(UserRepository users, PasswordEncoder passwordEncoder) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public User signUp(String rawEmail, String rawPassword, String name) {
    Email email = Email.of(rawEmail);
    PasswordPolicy.check(rawPassword);

    if (users.existsByEmail_Value(email.value())) {
      throw new EmailAlreadyTakenException(email.value());
    }

    String passwordHash = passwordEncoder.encode(rawPassword);
    User user = User.register(email, passwordHash, name);
    try {
      return users.save(user);
    } catch (DataIntegrityViolationException race) {
      // Lost the race to the unique index — same outcome as the pre-check.
      throw new EmailAlreadyTakenException(email.value());
    }
  }
}
