####
# Multi-stage Dockerfile for building and running Quarkus native application
#
# Stage 1: Build the native executable using GraalVM
# Stage 2: Run the native executable in a minimal container
#
# Build with:
#   docker build -t realworld-api:native .
#
# Run with:
#   docker run -i --rm -p 8080:8080 \
#     -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/realworld \
#     -e QUARKUS_DATASOURCE_USERNAME=realworld \
#     -e QUARKUS_DATASOURCE_PASSWORD=realworld \
#     realworld-api:native
###

# Stage 1: Build native executable
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-25 AS builder

USER root
WORKDIR /build

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY gradle.properties .
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Copy database migrations (needed for jOOQ code generation)
COPY db db

# Copy source code
COPY src src

# Make gradlew executable
RUN chmod +x gradlew

# Build native executable (use quarkusBuild to avoid linting)
RUN ./gradlew quarkusBuild -Dquarkus.package.type=native

# Stage 2: Runtime
FROM quay.io/quarkus/quarkus-micro-image:2.0

WORKDIR /work/
RUN chown 1001 /work \
    && chmod "g+rwX" /work \
    && chown 1001:root /work

# Copy native executable from builder
COPY --from=builder --chown=1001:root --chmod=0755 /build/build/*-runner /work/application

EXPOSE 8080
USER 1001

ENTRYPOINT ["./application", "-Dquarkus.http.host=0.0.0.0"]
