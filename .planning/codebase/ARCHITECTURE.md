# Architecture

**Analysis Date:** 2026-03-13

## Architectural Pattern

**DDD-lite with CQRS** — Domain-Driven Design with Command Query Responsibility Segregation.

- **Domain layer**: Entities with typed IDs, aggregate roots with invariants, repository interfaces
- **Application layer**: Split into WriteService (commands) and ReadService (queries)
- **Infrastructure layer**: jOOQ repository implementations, REST resources (JAX-RS), read service implementations
- **Shared kernel**: Base classes, annotations, security, exceptions

## Layers

### Domain Layer (`{aggregate}/domain/`)

Pure domain model with no framework dependencies.

- **Entities** extend `Entity<ID>` abstract class (`src/main/kotlin/com/example/shared/domain/Entity.kt`)
- **Aggregate Roots** annotated with `@AggregateRoot` — enforce invariants in `init` blocks via `require()`
- **Value Objects** annotated with `@ValueObject` — typed IDs using `@JvmInline value class` (e.g., `UserId`, `ArticleId`, `CommentId`)
- **Repository interfaces** extend `Repository<T, ID>` — define `nextId()`, `create()`, `update()`, `findById()`

**Aggregates:**
| Aggregate | Root Entity | Typed ID | Repository |
|-----------|-------------|----------|------------|
| Article | `Article` | `ArticleId` | `ArticleRepository` |
| Comment | `Comment` | `CommentId` | `CommentRepository` |
| User | `User` | `UserId` | `UserRepository` |
| Profile | (no entity) | — | `FollowRepository` |

### Application Layer (`{aggregate}/application/`)

Orchestrates use cases. Split into write and read sides (CQRS).

**Write side:**
- Interface: `ArticleWriteService`, `UserWriteService`, `CommentWriteService`, `ProfileWriteService`
- Implementation: `Default*WriteService` (e.g., `DefaultArticleWriteService`)
- Annotated with `@WriteService` (custom stereotype = `@ApplicationScoped`)
- Methods are `@Transactional` — own the transaction boundary
- Primary validation layer — collects field errors, throws `ValidationException` (422)
- Returns entity IDs (not full entities)

**Read side:**
- Interface: `ArticleReadService`, `UserReadService`, `CommentReadService`, `ProfileReadService`
- Implementation: `Jooq*ReadService` (e.g., `JooqArticleReadService`) — directly queries via jOOQ
- Annotated with `@ReadService` (custom stereotype = `@ApplicationScoped`)
- No `@Transactional` needed
- Returns summary data classes: `ArticleSummary`, `UserSummary`, `CommentSummary`, `ProfileSummary`

### Infrastructure Layer (`{aggregate}/infrastructure/`)

Framework-dependent implementations.

**Resources (REST controllers):**
- Implement OpenAPI-generated interfaces (e.g., `ArticleResource : ArticlesApi`)
- `@ApplicationScoped` — Quarkus CDI
- Map between OpenAPI DTOs and application-layer summary types
- Security via `@RolesAllowed("user")` and `SecurityContext`
- Return OpenAPI-generated response types with fluent setters

**Repository implementations:**
- `Jooq*Repository` classes implement domain repository interfaces
- Use jOOQ `DSLContext` for type-safe SQL
- `multiset()` for nested collections (no N+1)
- Handle tag management (upsert + junction table)

## Data Flow

### Write Path (Command)
```
HTTP Request
  → Resource (extracts params from OpenAPI DTO)
    → WriteService (validates, coordinates)
      → Domain Entity (enforces invariants)
      → Repository (persists via jOOQ)
    → ReadService (hydrates for response)
  → Resource (maps summary → OpenAPI DTO)
← HTTP Response
```

### Read Path (Query)
```
HTTP Request
  → Resource (extracts query params)
    → ReadService (queries via jOOQ, returns summary)
  → Resource (maps summary → OpenAPI DTO)
← HTTP Response
```

## Key Abstractions

### Custom Stereotype Annotations
- `@AggregateRoot` — marks domain aggregate roots
- `@ValueObject` — marks value objects (typed IDs)
- `@WriteService` — stereotype combining `@ApplicationScoped` for command services
- `@ReadService` — stereotype combining `@ApplicationScoped` for query services

### Validation Layers (Defense in Depth)
| Layer | Purpose | Exception | HTTP |
|-------|---------|-----------|------|
| WriteService | User input → friendly errors | `ValidationException` | 422 |
| Entity init | Domain invariants (safety net) | `IllegalArgumentException` | 500 |
| Database | Structural integrity (last wall) | SQL constraint violation | 500 |

### Exception Handling
Domain exceptions with dedicated JAX-RS ExceptionMappers:
- `NotFoundException` → `NotFoundExceptionMapper` → 404
- `ValidationException` → `ValidationExceptionMapper` → 422
- `ForbiddenException` → `ForbiddenExceptionMapper` → 403
- `UnauthorizedException` → `UnauthorizedExceptionMapper` → 401
- `BadRequestException` → `BadRequestExceptionMapper` → 400
- `UnhandledExceptionMapper` → 500 (catch-all)

All mappers in `src/main/kotlin/com/example/shared/exceptions/`.

### Security
- **JWT authentication** via SmallRye JWT (`JwtService` at `src/main/kotlin/com/example/shared/security/JwtService.kt`)
- **SecurityContext** (`@RequestScoped`) — extracts `UserId` from JWT subject (`src/main/kotlin/com/example/shared/security/SecurityContext.kt`)
- **Rate limiting** — IP-based via `RateLimitFilter` + Caffeine cache (`src/main/kotlin/com/example/shared/security/RateLimitFilter.kt`)
- **Password hashing** — Argon2id via `PasswordHasher` (`src/main/kotlin/com/example/shared/security/PasswordHasher.kt`)
- **MDC logging** — `LoggingMdcFilter` adds request context to logs

### Cross-Cutting Concerns
- `StatusCodeResponseFilter` — adjusts HTTP status codes for RESTEasy Reactive (`src/main/kotlin/com/example/shared/StatusCodeResponseFilter.kt`)
- `SlugGenerator` — generates URL-friendly slugs with uniqueness check (`src/main/kotlin/com/example/shared/utils/SlugGenerator.kt`)

## Entry Points

### REST API
All endpoints defined in OpenAPI spec, implemented by Resource classes:
- `UserAndAuthenticationResource` → `/api/users`, `/api/user`
- `ArticleResource` → `/api/articles`
- `CommentResource` → `/api/articles/{slug}/comments`
- `ProfileResource` → `/api/profiles/{username}`
- `FavoriteResource` → `/api/articles/{slug}/favorite`
- `TagResource` → `/api/tags`

### Code Generation
- **OpenAPI** → `gradle generateApi` → generates API interfaces and model DTOs
- **jOOQ** → `gradle generateJooq` → generates type-safe table/record classes from PostgreSQL schema

### Database
- **Schema**: Atlas HCL (`db/schema.hcl`)
- **Migrations**: Atlas (`db/migrations/`)
- **Tables**: `users`, `articles`, `tags`, `article_tags`, `comments`, `favorites`, `follows`

---

*Architecture audit: 2026-03-13*
