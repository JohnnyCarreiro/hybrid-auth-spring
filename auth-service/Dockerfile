# syntax=docker/dockerfile:1
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle :auth-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/auth-service/build/libs/*.jar app.jar
EXPOSE 3333
ENTRYPOINT ["java", "-jar", "app.jar"]
