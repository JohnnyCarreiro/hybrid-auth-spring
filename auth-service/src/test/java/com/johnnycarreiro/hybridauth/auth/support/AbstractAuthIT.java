package com.johnnycarreiro.hybridauth.auth.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for auth integration tests (ADR-0001): a real Postgres via Testcontainers, MockMvc for the
 * web edge.
 *
 * <p>The container follows the <strong>singleton pattern</strong>: it is started once in a static
 * initializer and never stopped — Ryuk reaps it when the JVM exits. This is deliberate. The earlier
 * {@code @Testcontainers}/{@code @Container} lifecycle stops the container at the end of
 * <em>each</em> test class, which breaks as soon as a second IT class loads a fresh Spring context
 * bound to the now-stopped container's port (connection refused). One JVM-wide container, started
 * manually and shared across every IT, sidesteps that. {@code @ServiceConnection} still wires the
 * datasource so Flyway runs the migrations on context load.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractAuthIT {

  @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

  static {
    POSTGRES.start();
  }
}
