# Dev task runner (mirror of the justfile). Docker-based — no host JDK/Gradle needed.
# Everything that talks to Docker is `docker-` prefixed. Run `make` to list.

GRADLE_IMG := gradle:8.10.2-jdk21
GRADLE := docker run --rm --user $(shell id -u):$(shell id -g) -e HOME=/tmp \
	-e GRADLE_USER_HOME=/work/.gradle -v "$(CURDIR)":/work -w /work \
	$(GRADLE_IMG) gradle --no-daemon --console=plain
COMPOSE := docker compose

.DEFAULT_GOAL := help
.PHONY: help docker-build docker-test docker-clean docker-up docker-up-infra \
	docker-run docker-run-auth docker-run-resource docker-down docker-down-v \
	docker-logs docker-ps docker-psql-auth docker-psql-app health fmt check

help: ## List available targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

docker-build: ## Compile + test all modules (Dockerized Gradle)
	$(GRADLE) build

docker-test: ## Run tests only (Dockerized Gradle)
	$(GRADLE) test

docker-clean: ## Gradle clean (Dockerized)
	$(GRADLE) clean

docker-up: ## Build + start the FULL stack (Postgres, Redis, both services), detached
	$(COMPOSE) up -d --build

docker-up-infra: ## Start only Postgres + Redis, detached
	$(COMPOSE) up -d postgres redis

docker-run: ## Run BOTH services in the foreground (brings up their deps)
	$(COMPOSE) up --build auth-service resource-service

docker-run-auth: ## Run only auth-service in the foreground (brings up its deps)
	$(COMPOSE) up --build auth-service

docker-run-resource: ## Run only resource-service in the foreground (brings up its deps)
	$(COMPOSE) up --build resource-service

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

fmt: ## Format sources (Spotless) — lands in FEAT-002
	@echo "Spotless not wired yet — FEAT-002 (ci-pipeline)."

check: ## Lint/format check (Spotless) — lands in FEAT-002
	@echo "spotlessCheck not wired yet — FEAT-002 (ci-pipeline)."
