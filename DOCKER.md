# Docker Deployment Guide

This guide covers building and running the RealWorld API as a GraalVM native image using Docker.

## Overview

The application uses a multi-stage Docker build:
1. **Builder stage**: Builds the native executable using Mandrel (GraalVM)
2. **Runtime stage**: Runs the native binary in a minimal UBI micro image

## Prerequisites

- Docker installed and running
- At least 4GB RAM allocated to Docker
- Docker Compose (optional, for full stack)

## Quick Start with Docker Compose

The easiest way to run the entire stack (PostgreSQL + API):

```bash
# Build and start all services
docker-compose up --build

# Or run in detached mode
docker-compose up --build -d

# View logs
docker-compose logs -f api

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

The API will be available at http://localhost:8080

## Building the Docker Image Manually

Build the native image (this will take 5-10 minutes):

```bash
docker build -t realworld-api:native .
```

The build process:
1. Downloads dependencies
2. Compiles Kotlin code
3. Generates jOOQ classes
4. Builds native executable with GraalVM
5. Creates minimal runtime image (~100MB)

## Running the Container

### With Docker Compose (Recommended)

```bash
docker-compose up
```

### With Docker Run

First, ensure PostgreSQL is running:

```bash
# Start PostgreSQL
docker run -d \
  --name realworld-postgres \
  -e POSTGRES_DB=realworld \
  -e POSTGRES_USER=realworld \
  -e POSTGRES_PASSWORD=realworld \
  -p 5432:5432 \
  postgres:18

# Run the API
docker run -i --rm \
  --name realworld-api \
  -p 8080:8080 \
  -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/realworld \
  -e QUARKUS_DATASOURCE_USERNAME=realworld \
  -e QUARKUS_DATASOURCE_PASSWORD=realworld \
  realworld-api:native
```

**Note**: On Linux, use `--network host` or link containers instead of `host.docker.internal`.

## Configuration

Environment variables can be passed to override configuration:

```bash
docker run -i --rm \
  -p 8080:8080 \
  -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://postgres:5432/realworld \
  -e QUARKUS_DATASOURCE_USERNAME=myuser \
  -e QUARKUS_DATASOURCE_PASSWORD=mypass \
  -e QUARKUS_HTTP_PORT=8080 \
  -e QUARKUS_LOG_LEVEL=INFO \
  realworld-api:native
```

## Database Migrations

The application uses Atlas for database migrations. Before running the API:

1. Ensure PostgreSQL is running
2. Run migrations using the Atlas CLI or let the application handle it on startup

## Image Sizes

- **Builder image**: ~1.5GB (Mandrel + JDK + build tools)
- **Final runtime image**: ~100-150MB (minimal UBI + native binary)
- **Native executable**: ~80-100MB

## Performance Characteristics

Native executable benefits:
- **Startup time**: ~0.05s (vs ~2-3s for JVM)
- **Memory usage**: ~50MB RSS (vs ~200-300MB for JVM)
- **Image size**: ~100MB (vs ~300-400MB for JVM)

## Troubleshooting

### Build fails with "Out of memory"

Increase Docker memory allocation to at least 4GB:
- Docker Desktop: Settings → Resources → Memory

### Container exits immediately

Check logs:
```bash
docker logs realworld-api
```

Common issues:
- Database not accessible
- Missing environment variables
- Port 8080 already in use

### Database connection refused

Ensure:
- PostgreSQL container is running
- Network connectivity between containers
- Correct database URL (use container name in Docker Compose)

### User registration fails with JNA/Argon2 error

**Known Limitation**: The native image currently has an issue with Argon2 password hashing due to JNA (Java Native Access) native library requirements in GraalVM.

Error: `UnsatisfiedLinkError: Error looking up function 'argon2_encodedlen'`

**Workaround**: Use the JVM-based Docker image instead (see Alternative Dockerfiles section below) until this is resolved.

## Alternative Dockerfiles

Pre-built native executable (faster builds, requires local build):

```bash
# Build native executable locally
./gradlew build -Dquarkus.package.type=native

# Use existing Dockerfile
docker build -f src/main/docker/Dockerfile.native-micro -t realworld-api .
```

JVM-based image (faster builds, larger image):

```bash
# Build JVM package
./gradlew build

# Use JVM Dockerfile
docker build -f src/main/docker/Dockerfile.jvm -t realworld-api:jvm .
```

## Production Considerations

1. **Database migrations**: Run Atlas migrations before deploying new versions
2. **Health checks**: The API exposes health endpoints at `/q/health`
3. **Secrets**: Use Docker secrets or environment variables from secure stores
4. **Logging**: Configure structured logging for production
5. **Monitoring**: Integrate with Prometheus/Grafana for metrics

## Testing the API

Once running, test with:

```bash
# Health check
curl http://localhost:8080/q/health

# API documentation
open http://localhost:8080/q/swagger-ui

# Register a user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "user": {
      "email": "test@example.com",
      "username": "testuser",
      "password": "password123"
    }
  }'
```

## Clean Up

Remove all containers and volumes:

```bash
# Using Docker Compose
docker-compose down -v

# Manual cleanup
docker stop realworld-api realworld-postgres
docker rm realworld-api realworld-postgres
docker volume rm realworld-backend-quarkus_postgres_data
docker rmi realworld-api:native
```
