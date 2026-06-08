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

    // Persistence: this service owns the `app` database (ADR-0003); Flyway migrations (ADR-0004).
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Bean validation on inbound request DTOs (jakarta.validation) — the web edge rejects malformed
    // input with a 400 before a use case runs (mirrors the auth-service).
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Domain-owned identity (SDD-002): Project/Task aggregates mint their own UUID v7 (the mirrored
    // `app.users.id` is NOT minted here — it is the auth `sub`). `uuid-creator` supplies the
    // time-ordered (v7) algorithm the JDK lacks, exactly as the auth-service does (OQ-005).
    implementation("com.github.f4b6a3:uuid-creator:5.3.7")

    // Resource-server JWT validation (ADR-0005): the starter brings spring-security-web (the filter
    // chain) + Nimbus JOSE. The access JWT is verified LOCALLY against the auth-service's published
    // public JWKS — no shared secret (SRS+SAD §2.5 / SDD-001 §4 invariant 6). The hand-built
    // `JwksTokenVerifier` (in-memory key cache + refetch-on-rotation) plugs in as the JwtDecoder.
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-jose")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}
