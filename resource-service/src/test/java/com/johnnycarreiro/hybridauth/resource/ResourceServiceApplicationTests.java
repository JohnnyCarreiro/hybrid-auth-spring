package com.johnnycarreiro.hybridauth.resource;

import com.johnnycarreiro.hybridauth.resource.support.AbstractResourceIT;
import org.junit.jupiter.api.Test;

/** The Spring context boots (security chain + JPA + Flyway V1–V4) against a real Postgres. */
class ResourceServiceApplicationTests extends AbstractResourceIT {

  @Test
  void contextLoads() {}
}
