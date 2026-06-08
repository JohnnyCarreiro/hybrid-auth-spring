package com.johnnycarreiro.hybridauth.resource.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for resource-service integration tests (ADR-0001): a real Postgres ({@code app} DB) via
 * Testcontainers, MockMvc for the web edge, and {@link TestSecurityConfig} so the real security
 * chain runs against an in-test signing key.
 *
 * <p>Singleton container, started once and never stopped (the same rationale as the auth-service's
 * {@code AbstractAuthIT}): the per-class {@code @Testcontainers} lifecycle stops the container
 * after each class, which breaks the next IT's context (connection refused).
 * {@code @ServiceConnection} wires the datasource so Flyway runs V1–V4 on context load.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
public abstract class AbstractResourceIT {

  @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

  static {
    POSTGRES.start();
  }

  @Autowired protected MockMvc mockMvc;
  @Autowired protected TestTokens tokens;
  @Autowired protected ObjectMapper objectMapper;
}
