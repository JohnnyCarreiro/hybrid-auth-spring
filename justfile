# Dev task runner (mirror of the Makefile). Docker-based — no host JDK/Gradle needed.
# Everything that talks to Docker is `docker-` prefixed. `just` lists all recipes.

gradle_img := "gradle:8.10.2-jdk21"
_gradle := "docker run --rm --user $(id -u):$(id -g) -e HOME=/tmp -e GRADLE_USER_HOME=/work/.gradle -v \"$PWD\":/work -w /work " + gradle_img + " gradle --no-daemon --console=plain"

# List available recipes
default:
    @just --list

# Compile + test all modules (Dockerized Gradle)
docker-build:
    {{_gradle}} build

# Run tests only (Dockerized Gradle)
docker-test:
    {{_gradle}} test

# Gradle clean (Dockerized)
docker-clean:
    {{_gradle}} clean

# Build + start the FULL stack (Postgres, Redis, both services), detached
docker-up:
    docker compose up -d --build

# Start only Postgres + Redis, detached
docker-up-infra:
    docker compose up -d postgres redis

# Run BOTH services in the foreground (brings up their deps)
docker-run:
    docker compose up --build auth-service resource-service

# Run only auth-service in the foreground (brings up its deps)
docker-run-auth:
    docker compose up --build auth-service

# Run only resource-service in the foreground (brings up its deps)
docker-run-resource:
    docker compose up --build resource-service

# Run BOTH services on the HOST via bootRun, in parallel (needs JDK 21)
dev-run:
    ./gradlew --parallel :auth-service:bootRun :resource-service:bootRun

# Run auth-service on the HOST via bootRun — fast loop, hot reload (needs JDK 21)
dev-auth:
    ./gradlew :auth-service:bootRun

# Run resource-service on the HOST via bootRun — fast loop, hot reload (needs JDK 21)
dev-resource:
    ./gradlew :resource-service:bootRun

# Stop the stack
docker-down:
    docker compose down

# Stop the stack and wipe volumes (drops the databases)
docker-down-v:
    docker compose down -v

# Tail all logs
docker-logs:
    docker compose logs -f

# Show running services
docker-ps:
    docker compose ps

# Open psql in the auth database
docker-psql-auth:
    docker compose exec -e PGPASSWORD=auth postgres psql -U auth_user -d auth

# Open psql in the app database
docker-psql-app:
    docker compose exec -e PGPASSWORD=app postgres psql -U app_user -d app

# Curl both /health endpoints
health:
    @curl -fsS localhost:${AUTH_PORT:-3333}/health && echo "  <- auth-service" || echo "auth-service down"
    @curl -fsS localhost:${RESOURCE_PORT:-3334}/health && echo "  <- resource-service" || echo "resource-service down"

# Format sources (Spotless) — lands in FEAT-002
fmt:
    @echo "Spotless not wired yet — FEAT-002 (ci-pipeline)."

# Lint/format check (Spotless) — lands in FEAT-002
check:
    @echo "spotlessCheck not wired yet — FEAT-002 (ci-pipeline)."
