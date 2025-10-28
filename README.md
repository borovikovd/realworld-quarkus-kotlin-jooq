# ![RealWorld Example App](https://raw.githubusercontent.com/gothinkster/realworld/master/media/realworld.png)

> ### Quarkus + Kotlin codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec and API.

### [Demo](https://demo.realworld.build/)&nbsp;&nbsp;&nbsp;&nbsp;[RealWorld](https://github.com/gothinkster/realworld)

This codebase was created to demonstrate a fully fledged backend application built with **Quarkus** and **Kotlin** including CRUD operations, authentication, routing, pagination, and more.

We've gone to great lengths to adhere to the **Quarkus** and **Kotlin** community styleguides & best practices.

For more information on how this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

# How it works

This application is built with a **DDD-lite** architecture using **CQRS** patterns for clean separation of concerns:

## Technology Stack

- **[Quarkus 3.28.x](https://quarkus.io/)** - Supersonic Subatomic Java Framework
  - GraalVM native image support for blazing fast startup (<100ms) and low memory usage (<50MB)
  - Built-in dependency injection, REST, and security
- **[Kotlin 2.2.x](https://kotlinlang.org/)** - Modern, concise, null-safe JVM language
- **[jOOQ 3.19.x](https://www.jooq.org/)** - Type-safe SQL query builder (not JPA/Hibernate)
  - Compile-time query validation
  - Full SQL feature access with Kotlin code generation
- **[PostgreSQL 18](https://www.postgresql.org/)** - Production-grade relational database
- **[Atlas](https://atlasgo.io/)** - Modern database schema migration tool
- **[SmallRye JWT](https://smallrye.io/smallrye-jwt/)** - JWT authentication (RS256)
- **[Argon2](https://github.com/phc/phc-winner-argon2)** - Secure password hashing

## Architecture

### DDD-Lite with CQRS

**Feature-based packages** (one per aggregate):
```
com.example/
├── user/           # User aggregate (registration, authentication)
├── profile/        # Follow aggregate (user relationships)
├── article/        # Article aggregate (includes tags, favorites)
├── comment/        # Comment aggregate
├── query/          # Read model (optimized queries)
└── shared/         # Cross-cutting concerns (security, exceptions)
```

**CQRS Separation:**
- **Commands (Write)**: Defined in `*Service.kt`, enforce business rules, transactional
- **Queries (Read)**: Centralized in `QueryService.kt`, optimized with jOOQ multiset (no N+1 queries)

**Key Patterns:**
- Rich domain entities with behavior methods
- Repository pattern per aggregate root
- Exception-based error handling with JAX-RS mappers
- OpenAPI-first API design

## Project Structure

```
src/main/
├── kotlin/com/example/
│   ├── article/              # Article CRUD, tags, favorites
│   │   ├── Article.kt        # Domain entity
│   │   ├── ArticleService.kt # Business logic
│   │   └── ArticleResource.kt # REST API
│   ├── user/                 # User registration & authentication
│   ├── profile/              # User profiles & following
│   ├── comment/              # Article comments
│   └── shared/               # Security, exceptions, utilities
└── resources/
    ├── openapi.yaml          # API specification
    ├── application.properties # Quarkus configuration
    └── privateKey.pem        # JWT signing key

db/
├── schema.hcl                # Atlas HCL schema (source of truth)
└── migrations/               # Generated SQL migrations
```

# Getting started

## Prerequisites

- **Java 21** (JDK)
- **Docker** and **Docker Compose** (for PostgreSQL)
- **Gradle** (included via wrapper)

## Quick Start

### 1. Start PostgreSQL

```bash
docker-compose up -d postgres
```

### 2. Run Database Migrations

```bash
# Install Atlas CLI first: https://atlasgo.io/getting-started
atlas migrate apply --env local
```

### 3. Run in Development Mode

```bash
./gradlew quarkusDev
```

The application will start on **http://localhost:8080** with live reload enabled.

### 4. Access API Documentation

- **Swagger UI**: http://localhost:8080/swagger-ui/
- **OpenAPI Spec**: http://localhost:8080/q/openapi

## Running Tests

```bash
# Run all tests (includes Testcontainers integration tests)
./gradlew test

# Run specific test class
./gradlew test --tests ArticleServiceTest
```

## Building for Production

### JVM Mode

```bash
./gradlew build
java -jar build/quarkus-app/quarkus-run.jar
```

### Native Image (GraalVM)

```bash
# Requires GraalVM installed
./gradlew build -Dquarkus.package.type=native

# Or build in container (no local GraalVM needed)
./gradlew build -Dquarkus.package.type=native -Dquarkus.native.container-build=true

# Run native executable
./build/realworld-api-1.0.0-runner
```

**Performance characteristics:**
- Startup time: **~13ms** (vs 2-3s for JVM)
- Memory usage: **~50MB** RSS (vs 200-300MB for JVM)
- Container image: **~190MB** (vs 300-400MB for JVM)

## Docker Deployment

### Using Docker Compose (Recommended)

```bash
# Build and start all services (PostgreSQL + API)
docker-compose up --build

# Run in detached mode
docker-compose up --build -d

# View logs
docker-compose logs -f api

# Stop all services
docker-compose down
```

### Manual Docker Build

```bash
# Build native image (takes 5-10 minutes)
docker build -t realworld-api:native .

# Run container
docker run -i --rm -p 8080:8080 \
  -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/realworld \
  -e QUARKUS_DATASOURCE_USERNAME=realworld \
  -e QUARKUS_DATASOURCE_PASSWORD=realworld \
  realworld-api:native
```

## Configuration

### Environment Variables

```bash
# Database
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:5432/realworld
QUARKUS_DATASOURCE_USERNAME=realworld
QUARKUS_DATASOURCE_PASSWORD=realworld

# JWT
MP_JWT_VERIFY_PUBLICKEY_LOCATION=classpath:publicKey.pem
MP_JWT_VERIFY_ISSUER=https://realworld.io

# Logging
QUARKUS_LOG_LEVEL=INFO
```

### Database Setup

The project uses [Atlas](https://atlasgo.io/) for schema migrations:

```bash
# View migration status
atlas migrate status --env local

# Generate new migration after editing db/schema.hcl
atlas migrate diff add_column --env local

# Apply migrations
atlas migrate apply --env local

# Regenerate jOOQ code after schema changes
./gradlew generateJooq
```

## Development Workflow

### Code Generation

```bash
# Generate OpenAPI client code (after editing openapi.yaml)
./gradlew generateApi

# Generate jOOQ code (after schema changes)
./gradlew generateJooq

# Format code with ktlint
./gradlew ktlintFormat

# Run all checks (tests + linting)
./gradlew build
```

### Development Mode Features

```bash
./gradlew quarkusDev
```

- **Live reload**: Code changes automatically reflected
- **Dev UI**: http://localhost:8080/q/dev/
- **Continuous testing**: Press `r` to run tests
- **Database**: Automatically connects to local PostgreSQL

## API Overview

The API implements the complete [RealWorld API spec](https://realworld-docs.netlify.app/):

### Authentication

- `POST /api/users` - Register user
- `POST /api/users/login` - Login user
- `GET /api/user` - Get current user
- `PUT /api/user` - Update user

### Profiles

- `GET /api/profiles/:username` - Get user profile
- `POST /api/profiles/:username/follow` - Follow user
- `DELETE /api/profiles/:username/follow` - Unfollow user

### Articles

- `GET /api/articles` - List articles (with filters)
- `GET /api/articles/feed` - Get user feed
- `GET /api/articles/:slug` - Get article
- `POST /api/articles` - Create article
- `PUT /api/articles/:slug` - Update article
- `DELETE /api/articles/:slug` - Delete article
- `POST /api/articles/:slug/favorite` - Favorite article
- `DELETE /api/articles/:slug/favorite` - Unfavorite article

### Comments

- `GET /api/articles/:slug/comments` - Get comments
- `POST /api/articles/:slug/comments` - Add comment
- `DELETE /api/articles/:slug/comments/:id` - Delete comment

### Tags

- `GET /api/tags` - Get all tags

## Example Usage

```bash
# Register a user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "user": {
      "email": "jake@example.com",
      "username": "jake",
      "password": "jakejake"
    }
  }'

# Login
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "user": {
      "email": "jake@example.com",
      "password": "jakejake"
    }
  }'

# Create article (with auth token)
curl -X POST http://localhost:8080/api/articles \
  -H "Content-Type: application/json" \
  -H "Authorization: Token <your-jwt-token>" \
  -d '{
    "article": {
      "title": "How to train your dragon",
      "description": "Ever wonder how?",
      "body": "You have to believe",
      "tagList": ["dragons", "training"]
    }
  }'

# List articles
curl http://localhost:8080/api/articles
```

## Code Quality

```bash
# Format code
./gradlew ktlintFormat

# Check code style
./gradlew ktlintCheck

# Run tests
./gradlew test

# Full build (compilation + tests + linting)
./gradlew build
```

## Performance

The application is optimized for production:

- **No N+1 queries**: All nested data fetched using jOOQ multiset
- **Indexed queries**: Foreign keys and frequently queried columns indexed
- **Connection pooling**: Agroal connection pool (10-50 connections)
- **Pagination**: Limit/offset on all list endpoints
- **Fast startup**: GraalVM native image starts in ~13ms

## Troubleshooting

### Database connection refused

Ensure PostgreSQL is running:
```bash
docker-compose up -d postgres
docker-compose ps
```

### Port 8080 already in use

Change the port in `application.properties`:
```properties
quarkus.http.port=8081
```

### jOOQ classes not found

Regenerate jOOQ code:
```bash
./gradlew generateJooq
```

### Native image build fails

Ensure you have at least 4GB RAM allocated to Docker:
- Docker Desktop: Settings → Resources → Memory

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License.

## Resources

- [Quarkus Documentation](https://quarkus.io/guides/)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [jOOQ Manual](https://www.jooq.org/doc/latest/manual/)
- [RealWorld Spec](https://realworld-docs.netlify.app/)
- [Atlas Documentation](https://atlasgo.io/docs)
