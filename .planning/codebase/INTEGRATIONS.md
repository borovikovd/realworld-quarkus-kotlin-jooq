# External Integrations

**Analysis Date:** 2026-03-13

## APIs & External Services

**None detected.** The codebase does not integrate with external APIs (Stripe, SendGrid, etc.). It is a self-contained backend service.

## Data Storage

**Databases:**
- PostgreSQL 18
  - Connection: Via JDBC (managed by Quarkus datasource)
  - Connection URL env var: `QUARKUS_DATASOURCE_JDBC_URL`
  - Default: `jdbc:postgresql://localhost:5432/realworld`
  - Client: jOOQ 3.20.8 (type-safe query builder)
  - ORM: None (direct SQL via jOOQ)
  - Connection pool: Min 5, Max 20 connections
  - Credentials: Username and password via env vars `QUARKUS_DATASOURCE_USERNAME`, `QUARKUS_DATASOURCE_PASSWORD`

**Schema Management:**
- Atlas (HCL-based DDL)
  - Source: `db/schema.hcl` defines desired schema
  - Migrations: `db/migrations/*.sql` (generated and committed)
  - Workflow: Edit HCL → `atlas migrate diff <name>` → `atlas migrate apply`
  - jOOQ regeneration: After migrations, run `gradle generateJooq`
  - Never commit generated code in `build/generated/`

**File Storage:**
- Local filesystem only - User profile images stored as URLs in `users.image` field

**Caching:**
- Caffeine (in-memory cache)
  - Used for rate limiting implementation
  - No external cache service (Redis/Memcached)

## Authentication & Identity

**Auth Provider:**
- Custom JWT-based (no third-party auth service)
  - Implementation: SmallRye JWT extension
  - Token generation: `src/main/kotlin/com/example/shared/security/JwtService.kt`
  - Token verification: Via JWT filter (Quarkus built-in)

**JWT Configuration:**
- Issuer: `https://realworld.io`
- Token lifespan: 60 days (5184000 seconds)
- Signing/verification keys: RSA keys stored in classpath
  - Public key: `src/main/resources/public.pem`
  - Private key: `src/main/resources/private.pem`
  - Env vars: `mp.jwt.verify.publickey.location`, `smallrye.jwt.sign.key.location`

**Password Hashing:**
- Argon2id (BouncyCastle implementation)
  - Location: `src/main/kotlin/com/example/shared/security/PasswordHasher.kt`
  - Parameters: ITERATIONS=10, MEMORY_KB=65536, PARALLELISM=1
  - Salt length: 32 bytes
  - Hash length: 64 bytes

## Monitoring & Observability

**Metrics Export:**
- Prometheus metrics endpoint
  - Extension: `quarkus-micrometer-registry-prometheus`
  - Endpoint: `/q/metrics` (Quarkus default)
  - Metrics collected: JVM, HTTP requests, connection pools, custom business metrics

**Health Checks:**
- SmallRye Health extension
  - Endpoints: `/q/health` (main), `/q/health/live` (liveness), `/q/health/ready` (readiness)
  - Used for Kubernetes liveness/readiness probes

**Logs:**
- Structured JSON logging (disabled in dev/test for readability)
  - Extension: `quarkus-logging-json`
  - Output format: JSON with service metadata
  - Service identifier: `"service": "realworld-api"` in JSON logs
  - Categories logged at DEBUG level: `com.example.*`, `org.jooq`
  - Format in human-readable mode: `%d{HH:mm:ss} %-5p [%c{2.}] (%t) %s%e%n`

**Distributed Tracing:**
- OpenTelemetry support (disabled by default)
  - Extension: `quarkus-opentelemetry`
  - Enable: Set env var `QUARKUS_OTEL_SDK_DISABLED=false`
  - Exporter: OTLP endpoint at `http://localhost:4317` (configurable)
  - Must be explicitly enabled in production configuration

**Error Tracking:**
- None configured - Errors logged to stdout/files only

## CI/CD & Deployment

**Hosting:**
- Docker-based deployment (JVM or native binary)
- Kubernetes-ready (health checks available)
- Docker Compose for local development
  - API service: Port 8080
  - PostgreSQL service: Port 5432

**CI Pipeline:**
- None detected - No GitHub Actions, GitLab CI, etc. configured

**Build Process:**
- Gradle build system with two modes:
  1. JVM mode: `gradle build` → `build/quarkus-app/quarkus-run.jar`
  2. Native mode: `gradle build -Dquarkus.package.type=native` → native binary

**Code Quality Gates:**
- ktlint: Kotlin formatting/linting (runs on compilation)
- Detekt: Static analysis for code smells
- SpotBugs: Bytecode security analysis (with FindSecBugs plugin)
- OWASP Dependency-Check: CVE scanning (fails build on CVSS 7.0+)
- All check tasks run as part of `gradle build`

## Environment Configuration

**Required env vars:**
- `QUARKUS_DATASOURCE_JDBC_URL` - PostgreSQL connection string
- `QUARKUS_DATASOURCE_USERNAME` - Database user
- `QUARKUS_DATASOURCE_PASSWORD` - Database password
- `NVD_API_KEY` (optional) - OWASP Dependency-Check API key for faster scanning
- `QUARKUS_OTEL_SDK_DISABLED` (optional) - Enable OpenTelemetry (set to `false`)

**Defaults Provided:**
- Development: `jdbc:postgresql://localhost:5432/realworld`
- Test: `jdbc:postgresql://localhost:5433/realworld_test`
- Both use credentials: `realworld:realworld`

**Secrets Location:**
- RSA key files: `src/main/resources/` (`public.pem`, `private.pem`)
- Database credentials: Environment variables (injected at runtime, not committed)
- JWT issuer/lifespan: `application.properties` (non-secret configuration)

## Webhooks & Callbacks

**Incoming:**
- None - API is request-response only

**Outgoing:**
- None - No outbound webhooks or callbacks

**External API Consumption:**
- None - No dependency on external APIs

## Data Flow Overview

**User Registration/Login:**
1. Client POST to `/api/users` or `/api/users/login`
2. Resource layer validates and delegates to UserWriteService
3. UserWriteService hashes password with Argon2id, stores in PostgreSQL
4. JwtService generates JWT token with user claims (userId, email, username)
5. Token returned in response

**Authenticated Requests:**
1. Client includes `Authorization: Bearer <token>` header
2. Quarkus JWT filter validates token signature and expiry
3. SecurityContext extracts userId from token claims
4. Service methods access current user via `SecurityContext.requireCurrentUserId()`

**Database Queries:**
1. Services use jOOQ to build type-safe queries
2. Queries execute against PostgreSQL via JDBC connection pool
3. jOOQ `multiset()` prevents N+1 queries by fetching nested data in single statement

---

*Integration audit: 2026-03-13*
