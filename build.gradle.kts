plugins {
    java
    alias(libs.plugins.spotless) apply false
}

allprojects {
    group = "com.johnnycarreiro.hybridauth"
    version = "0.3.0" // x-release-please-version

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            googleJavaFormat()
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        // On very recent Docker Engines, Testcontainers' docker-java client pings with an old API version
        // (1.32) the daemon rejects ("minimum supported API version is 1.40"). DOCKER_HOST selects the
        // env-driven client strategy; the API version, however, is read by docker-java from the
        // `api.version` *system property* — NOT the DOCKER_API_VERSION env — so it is forwarded as such.
        // Both only forwarded when the host sets them, so CI on older daemons keeps auto-negotiating.
        System.getenv("DOCKER_HOST")?.let { environment("DOCKER_HOST", it) }
        System.getenv("DOCKER_API_VERSION")?.let { systemProperty("api.version", it) }
    }
}
