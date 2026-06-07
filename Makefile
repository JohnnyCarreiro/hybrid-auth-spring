# Dev task runner (mirror of the justfile). Docker-based — no host JDK/Gradle needed.
# Everything that talks to Docker is `docker-` prefixed. Run `make` to list.

GRADLE_IMG := gradle:8.10.2-jdk21
GRADLE := docker run --rm --user $(shell id -u):$(shell id -g) -e HOME=/tmp \
	-e GRADLE_USER_HOME=/work/.gradle -v "$(CURDIR)":/work -w /work \
	$(GRADLE_IMG) gradle --no-daemon --console=plain
COMPOSE := docker compose

.DEFAULT_GOAL := help
.PHONY: help docker-build docker-clean docker-up docker-up-infra \
	docker-run docker-run-auth docker-run-resource docker-down docker-down-v \
	docker-logs docker-ps docker-psql-auth docker-psql-app dev-run dev-auth dev-resource \
	test health fmt check hooks-install

help: ## List available targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

docker-build: ## Compile + package the bootJars (Dockerized; no tests — see `test`)
	$(GRADLE) assemble

docker-clean: ## Gradle clean (Dockerized)
	$(GRADLE) clean

test: ## Run the full test suite on the HOST (Testcontainers spins its own DBs; needs JDK 21 + Docker)
	./gradlew test

docker-up: ## Build + start the FULL stack (Postgres, Redis, both services), detached
	$(COMPOSE) up -d --build

docker-up-infra: ## Start only Postgres + Redis (waits until healthy)
	$(COMPOSE) up -d --wait postgres redis

docker-run: ## Run BOTH services in the foreground (brings up their deps)
	$(COMPOSE) up --build auth-service resource-service

docker-run-auth: ## Run only auth-service in the foreground (brings up its deps)
	$(COMPOSE) up --build auth-service

docker-run-resource: ## Run only resource-service in the foreground (brings up its deps)
	$(COMPOSE) up --build resource-service

dev-run: docker-up-infra ## Run BOTH services on the HOST via bootRun, in parallel (needs JDK 21)
	./gradlew --parallel :auth-service:bootRun :resource-service:bootRun

dev-auth: docker-up-infra ## Run auth-service on the HOST via bootRun — fast loop, hot reload (needs JDK 21)
	./gradlew :auth-service:bootRun

dev-resource: docker-up-infra ## Run resource-service on the HOST via bootRun — fast loop, hot reload (needs JDK 21)
	./gradlew :resource-service:bootRun

docker-down: ## Stop the stack
	$(COMPOSE) down

docker-down-v: ## Stop the stack and wipe volumes (drops the databases)
	$(COMPOSE) down -v

docker-logs: ## Tail all logs
	$(COMPOSE) logs -f

docker-ps: ## Show running services
	$(COMPOSE) ps

docker-psql-auth: ## Open psql in the auth database
	$(COMPOSE) exec -e PGPASSWORD=auth postgres psql -U auth_user -d auth

docker-psql-app: ## Open psql in the app database
	$(COMPOSE) exec -e PGPASSWORD=app postgres psql -U app_user -d app

health: ## Curl both /health endpoints
	@curl -fsS localhost:$${AUTH_PORT:-3333}/health && echo "  <- auth-service" || echo "auth-service down"
	@curl -fsS localhost:$${RESOURCE_PORT:-3334}/health && echo "  <- resource-service" || echo "resource-service down"

fmt: ## Format sources (Spotless / google-java-format, Dockerized)
	$(GRADLE) spotlessApply

check: ## Lint/format check (Spotless, Dockerized)
	$(GRADLE) spotlessCheck

hooks-install: ## Install local git hooks (plain POSIX, no deps)
	@chmod +x .githooks/*
	@git config core.hooksPath .githooks
	@echo "hooks installed (core.hooksPath=.githooks)"
