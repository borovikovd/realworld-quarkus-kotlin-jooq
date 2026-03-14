# Project Structure

**Analysis Date:** 2026-03-13

## Directory Layout

```
realworld-backend-quarkus/
├── build.gradle.kts                    # Build config, plugins, dependencies
├── settings.gradle.kts                 # Project name, plugin management
├── gradle.properties                   # Quarkus platform version
├── openapi.yaml                        # API specification (source of truth)
├── db/
│   ├── schema.hcl                      # Atlas database schema (HCL format)
│   └── migrations/                     # Atlas-generated SQL migrations
├── src/
│   ├── main/
│   │   ├── kotlin/com/example/
│   │   │   ├── article/                # Article aggregate
│   │   │   │   ├── domain/             # Entity, typed ID, repository interface
│   │   │   │   ├── application/        # Write/Read service interfaces + implementations
│   │   │   │   └── infrastructure/     # jOOQ repos, REST resources, read services
│   │   │   ├── comment/                # Comment aggregate (same structure)
│   │   │   │   ├── domain/
│   │   │   │   ├── application/
│   │   │   │   └── infrastructure/
│   │   │   ├── profile/                # Profile aggregate (same structure)
│   │   │   │   ├── domain/
│   │   │   │   ├── application/
│   │   │   │   └── infrastructure/
│   │   │   ├── user/                   # User aggregate (same structure)
│   │   │   │   ├── domain/
│   │   │   │   ├── application/
│   │   │   │   └── infrastructure/
│   │   │   └── shared/                 # Shared kernel
│   │   │       ├── architecture/       # DDD annotations (@AggregateRoot, @WriteService, etc.)
│   │   │       ├── domain/             # Base Entity<ID>, Repository<T,ID>
│   │   │       ├── exceptions/         # Domain exceptions + JAX-RS mappers
│   │   │       ├── security/           # JWT, auth, rate limiting, password hashing
│   │   │       ├── utils/              # SlugGenerator
│   │   │       └── StatusCodeResponseFilter.kt
│   │   └── resources/
│   │       └── application.properties  # Quarkus configuration
│   └── test/
│       └── kotlin/com/example/
│           ├── archunit/               # Architecture fitness tests
│           ├── article/                # Article tests (domain, application, API)
│           ├── comment/                # Comment tests
│           ├── profile/                # Profile tests
│           ├── user/                   # User tests
│           └── shared/                 # Test utilities, fixtures, Testcontainers
├── config/
│   └── spotbugs/                       # Static analysis config
├── owasp/                              # OWASP dependency check suppressions
└── zap/                                # ZAP security scan config
```

## Key File Locations

### Per-Aggregate Pattern
Each aggregate follows the same 3-layer structure:

| Layer | File Pattern | Example |
|-------|-------------|---------|
| Domain entity | `{agg}/domain/{Entity}.kt` | `article/domain/Article.kt` |
| Typed ID | `{agg}/domain/{Entity}Id.kt` | `article/domain/ArticleId.kt` |
| Repository interface | `{agg}/domain/{Entity}Repository.kt` | `article/domain/ArticleRepository.kt` |
| Write service interface | `{agg}/application/{Entity}WriteService.kt` | `article/application/ArticleWriteService.kt` |
| Write service impl | `{agg}/application/Default{Entity}WriteService.kt` | `article/application/DefaultArticleWriteService.kt` |
| Read service interface | `{agg}/application/{Entity}ReadService.kt` | `article/application/ArticleReadService.kt` |
| Read summary DTO | `{agg}/application/{Entity}Summary.kt` | `article/application/ArticleSummary.kt` |
| Repository impl | `{agg}/infrastructure/Jooq{Entity}Repository.kt` | `article/infrastructure/JooqArticleRepository.kt` |
| Read service impl | `{agg}/infrastructure/Jooq{Entity}ReadService.kt` | `article/infrastructure/JooqArticleReadService.kt` |
| REST resource | `{agg}/infrastructure/{Entity}Resource.kt` | `article/infrastructure/ArticleResource.kt` |

### Shared Kernel
| Purpose | File |
|---------|------|
| Entity base class | `shared/domain/Entity.kt` |
| Repository base interface | `shared/domain/Repository.kt` |
| Aggregate root annotation | `shared/architecture/AggregateRoot.kt` |
| Value object annotation | `shared/architecture/ValueObject.kt` |
| Write service annotation | `shared/architecture/WriteService.kt` |
| Read service annotation | `shared/architecture/ReadService.kt` |
| JWT service | `shared/security/JwtService.kt` |
| Security context | `shared/security/SecurityContext.kt` |
| Password hasher | `shared/security/PasswordHasher.kt` |
| Rate limiter | `shared/security/RateLimiter.kt` |
| Rate limit filter | `shared/security/RateLimitFilter.kt` |
| MDC logging filter | `shared/security/LoggingMdcFilter.kt` |
| Slug generator | `shared/utils/SlugGenerator.kt` |
| Exception classes | `shared/exceptions/{Exception}.kt` |
| Exception mappers | `shared/exceptions/{Exception}Mapper.kt` |

### Test Structure
| Type | Location | Example |
|------|----------|---------|
| Domain unit tests | `test/{agg}/domain/{Entity}Test.kt` | `article/domain/ArticleTest.kt` |
| Service unit tests | `test/{agg}/application/Default{Entity}WriteServiceTest.kt` | `article/application/DefaultArticleWriteServiceTest.kt` |
| API integration tests | `test/{agg}/infrastructure/{Entity}ApiTest.kt` | `article/infrastructure/ArticleApiTest.kt` |
| Architecture tests | `test/archunit/*.kt` | `ArchitectureTest.kt`, `LayerDependencyRules.kt` |
| Test utilities | `test/shared/` | `BaseApiTest.kt`, `ApiTestFixtures.kt`, `TestDataBuilder.kt` |
| Testcontainers setup | `test/shared/PostgresTestResource.kt` | — |

### Configuration & Build
| Purpose | File |
|---------|------|
| Build config | `build.gradle.kts` |
| Quarkus config | `src/main/resources/application.properties` |
| OpenAPI spec | `openapi.yaml` |
| DB schema (HCL) | `db/schema.hcl` |
| DB migrations | `db/migrations/` |

## Naming Conventions

### Classes
| Pattern | Convention | Example |
|---------|-----------|---------|
| Domain entity | PascalCase noun | `Article`, `User`, `Comment` |
| Typed ID | `{Entity}Id` | `ArticleId`, `UserId` |
| Repository interface | `{Entity}Repository` | `ArticleRepository` |
| Repository impl | `Jooq{Entity}Repository` | `JooqArticleRepository` |
| Write service interface | `{Entity}WriteService` | `ArticleWriteService` |
| Write service impl | `Default{Entity}WriteService` | `DefaultArticleWriteService` |
| Read service interface | `{Entity}ReadService` | `ArticleReadService` |
| Read service impl | `Jooq{Entity}ReadService` | `JooqArticleReadService` |
| REST resource | `{Entity}Resource` | `ArticleResource` |
| Summary DTO | `{Entity}Summary` | `ArticleSummary` |
| Exception | `{Type}Exception` | `NotFoundException`, `ValidationException` |
| Exception mapper | `{Type}ExceptionMapper` | `NotFoundExceptionMapper` |

### Packages
- `com.example.{aggregate}.domain` — domain model
- `com.example.{aggregate}.application` — use cases
- `com.example.{aggregate}.infrastructure` — framework implementations
- `com.example.shared.*` — shared kernel

## Where to Add New Code

**New aggregate:** Create `{aggregate}/domain/`, `{aggregate}/application/`, `{aggregate}/infrastructure/` following the existing pattern.

**New endpoint:** Add to `openapi.yaml` → `gradle generateApi` → implement the generated interface in a new Resource class.

**New database table:** Edit `db/schema.hcl` → `atlas migrate diff` → `atlas migrate apply` → `gradle generateJooq`.

**New exception type:** Add exception class + mapper in `shared/exceptions/`.

**New shared utility:** Add to `shared/utils/` or appropriate `shared/` subpackage.

---

*Structure audit: 2026-03-13*
