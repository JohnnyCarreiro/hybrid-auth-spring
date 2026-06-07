plugins {
    `java-library`
}

// Cross-service contracts (JWT claim names, shared DTOs). Kept dependency-free on purpose:
// it is introduced only when it prevents real duplication, never preemptively (SAD §2.2).
