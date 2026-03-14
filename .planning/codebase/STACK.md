# Technology Stack

**Analysis Date:** 2026-03-13

## Languages

**Primary:**
- Kotlin 2.3.10 - Server-side application logic
- Java 21 - Compilation target and runtime

**Secondary:**
- SQL - Database migrations and jOOQ generated code

## Runtime

**Environment:**
- Java 21 (OpenJDK)
- Quarkus 3.32.3 - Reactive web framework

**Package Manager:**
- Gradle 8.x (via Gradle Wrapper)
- Lockfile: Present (gradle.properties)

## Frameworks

**Core:**
- Quarkus 3.32.3 - Cloud-native Java framework with native compilation support
- Quarkus REST - RESTful API layer
- Quarkus REST Jackson - JSON marshalling/unmarshalling

**Authentication & Security:**
- SmallRye JWT 3.x (via Quarkus BOM) - JWT token generation and validation
- BouncyCastle 1.83 - Cryptographic algorithms (Argon2id password hashing)

**Data Access:**
- jOOQ 3.20.8 - Type-safe SQL query builder
- Quarkus jOOQ 2.1.0 - Quarkus integration for jOOQ
- PostgreSQL JDBC Driver (via Quarkus BOM) - Database connectivity

**Database Migrations:**
- Atlas (HCL-based) - Declarative schema management
- Source: `db/schema.hcl` (HCL schema definition)
- Migrations stored: `db/migrations/*.sql`

**API & Code Generation:**
- OpenAPI Generator 7.20.0 - REST API interface generation from `openapi.yaml`
- SmallRye OpenAPI - OpenAPI/Swagger specification support
- Swagger UI - Interactive API documentation

**Testing:**
- Quarkus JUnit5 - Test runner
- Testcontainers 2.0.3 - Container-based integration testing (PostgreSQL)
- MockK 1.14.9 - Kotlin mocking framework
- REST Assured - HTTP API testing
- ArchUnit 1.4.1 - Architecture validation testing

**Build/Dev:**
- Kotlin Gradle Plugin 2.3.10 - Kotlin compilation
- Kotlin AllOpen Plugin - Opens classes/methods for frameworks requiring it
- jOOQ Gradle Plugin 10.2 - SQL code generation
- OpenAPI Generator Gradle Plugin 7.20.0 - API code generation

## Key Dependencies

**Critical:**
- `io.quarkus:quarkus-rest` - REST endpoint implementation
- `io.quarkus:quarkus-jdbc-postgresql` - Database connectivity
- `io.quarkus:quarkus-smallrye-jwt` - JWT token handling
- `io.quarkiverse.jooq:quarkus-jooq:2.1.0` - Type-safe query builder

**Observability:**
- `io.quarkus:quarkus-logging-json` - Structured JSON logging
- `io.quarkus:quarkus-micrometer-registry-prometheus` - Prometheus metrics export
- `io.quarkus:quarkus-smallrye-health` - Health check endpoints
- `io.quarkus:quarkus-opentelemetry` - Distributed tracing support (disabled by default)

**Rate Limiting:**
- `io.quarkus:quarkus-caffeine` - In-memory cache for rate limiting

**Security Analysis:**
- SpotBugs 6.4.8 - Bytecode static analysis with FindSecBugs plugin
- OWASP Dependency-Check 12.2.0 - Software composition analysis (CVE scanning)

**Code Quality:**
- ktlint 1.5.0 - Kotlin code formatting and linting
- Detekt 1.23.8 - Static analysis for code smells and complexity

## Configuration

**Environment:**
- `application.properties` - Base configuration
- Profile-specific overrides:
  - `%dev.` prefix for development mode
  - `%test.` prefix for test mode
- Environment variables override property file values

**Key Configs:**
- PostgreSQL JDBC URL: `quarkus.datasource.jdbc.url`
- Min/Max connection pool: `quarkus.datasource.jdbc.min-size=5`, `max-size=20`
- JWT signing/verification keys: Located in `src/main/resources/` (`private.pem`, `public.pem`)
- JWT issuer: `https://realworld.io`
- JWT token lifespan: 5184000 seconds (60 days)
- CORS configuration: Open to all origins with specific allowed methods and headers

**Build:**
- `build.gradle.kts` - Primary build configuration
- `gradle.properties` - Quarkus version and plugin versions
- `settings.gradle.kts` - Project setup and plugin management
- `.gitignore` includes: `build/`, `.gradle/`, `.idea/`, `.quarkus/`

## Platform Requirements

**Development:**
- Java 21+ (OpenJDK compatible)
- Gradle wrapper (bundled)
- Docker + Docker Compose for PostgreSQL local development
- PostgreSQL 18 (via Docker Compose or local installation)

**Production:**
- Java 21 runtime OR native compiled binary
- PostgreSQL 18+ database
- 512 MB minimum heap for JVM mode (1 GB recommended)
- Native compilation requires GraalVM/Mandrel JDK 21

## Deployment

**Containerization:**
- Multi-stage Dockerfile for native compilation
- Build image: `quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21`
- Runtime image: `quay.io/quarkus/quarkus-micro-image:2.0`
- Executable: Native binary (sub-100MB typical)

**Local Development Stack:**
- Docker Compose with PostgreSQL 18 service
- JVM mode with Quarkus dev server (hot reload available via `gradle quarkusDev`)

---

*Stack analysis: 2026-03-13*
