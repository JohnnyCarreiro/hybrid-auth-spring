# syntax=docker/dockerfile:1
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle :resource-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/resource-service/build/libs/*.jar app.jar
EXPOSE 3334
ENTRYPOINT ["java", "-jar", "app.jar"]
