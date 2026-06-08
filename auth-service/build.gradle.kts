plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":shared"))

    // Web on Jetty instead of the default Tomcat (ADR-0004).
    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }
    implementation("org.springframework.boot:spring-boot-starter-jetty")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Persistence: this service owns the `auth` database (ADR-0003); Flyway migrations (ADR-0004).
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Bean validation on inbound request DTOs (jakarta.validation).
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Identity (F1): the User aggregate mints its own UUID v7 — domain-owned identity, not
    // delegated to the ORM. `uuid-creator` provides the time-ordered (v7) algorithm the JDK lacks.
    implementation("com.github.f4b6a3:uuid-creator:5.3.7")

    // JWKS + RS256 issuer (F2): Nimbus JOSE via Spring Security's OAuth2 JOSE module — gives RSAKey,
    // JWKSet, JWKSource, NimbusJwtEncoder/Decoder. The auth-service signs; the resource-service later
    // verifies against the published public JWKS (no shared secret — SDD-001 §4 invariant 6).
    implementation("org.springframework.security:spring-security-oauth2-jose")

    // Password hashing: Argon2id (ADR-0002) via spring-security-crypto; BouncyCastle backs Argon2.
    implementation("org.springframework.security:spring-security-crypto")
    // BouncyCastle backs Argon2id; not managed by the Spring Boot BOM, so the version is explicit.
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}
