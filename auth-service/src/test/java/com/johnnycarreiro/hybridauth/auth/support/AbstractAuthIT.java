package com.johnnycarreiro.hybridauth.auth.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base for auth integration tests (ADR-0001): a real Postgres via Testcontainers, MockMvc for the
 * web edge. The container is {@code static}, so it boots once and is shared across every IT in the
 * JVM; {@code @ServiceConnection} wires the datasource and Flyway runs the migrations on context
 * load.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public abstract class AbstractAuthIT {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");
}
