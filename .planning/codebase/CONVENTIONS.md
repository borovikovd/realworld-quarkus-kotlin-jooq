# Coding Conventions

**Analysis Date:** 2026-03-13

## Language & Tooling

- **Language:** Kotlin 2.3.10
- **Formatting:** ktlint 14.1.0 (via `org.jlleitschuh.gradle.ktlint` plugin)
- **Static Analysis:** Detekt 1.23.8, SpotBugs 6.4.8
- **Architecture Tests:** ArchUnit — enforces naming, layer dependencies, DI scoping

## Naming Conventions

### Classes
| Type | Pattern | Example |
|------|---------|---------|
| Domain entity | PascalCase noun | `Article`, `User`, `Comment` |
| Typed ID | `{Entity}Id` (inline value class) | `ArticleId`, `UserId` |
| Repository interface | `{Entity}Repository` | `ArticleRepository` |
| Repository implementation | `Jooq{Entity}Repository` | `JooqArticleRepository` |
| Write service interface | `{Entity}WriteService` | `ArticleWriteService` |
| Write service implementation | `Default{Entity}WriteService` | `DefaultArticleWriteService` |
| Read service interface | `{Entity}ReadService` | `ArticleReadService` |
| Read service implementation | `Jooq{Entity}ReadService` | `JooqArticleReadService` |
| REST resource | `{Entity}Resource` | `ArticleResource` |
| Read-side DTO | `{Entity}Summary` | `ArticleSummary` |
| Exception | `{Type}Exception` | `NotFoundException` |
| Exception mapper | `{Type}ExceptionMapper` | `NotFoundExceptionMapper` |

Enforced by ArchUnit tests in `src/test/kotlin/com/example/archunit/NamingConventionRules.kt`.

### Packages
```
com.example.{aggregate}.domain          # Entities, value objects, repository interfaces
com.example.{aggregate}.application     # Service interfaces, implementations, DTOs
com.example.{aggregate}.infrastructure  # jOOQ repos, REST resources, read service impls
com.example.shared.*                    # Shared kernel
```

### Files
- One class per file (Kotlin convention)
- File name matches primary class name
- Private extension functions at file bottom (e.g., `toDto()` in Resource files)

## Code Patterns

### DDD Annotations
Custom stereotype annotations in `src/main/kotlin/com/example/shared/architecture/`:
- `@AggregateRoot` — marks domain aggregate roots
- `@ValueObject` — marks value objects (typed IDs)
- `@WriteService` — `@ApplicationScoped` stereotype for command services
- `@ReadService` — `@ApplicationScoped` stereotype for query services

### Domain Entities
- Extend `Entity<ID>` abstract class (identity-based equality)
- Invariants enforced via `require()` in `init` blocks
- Immutable — mutation returns new instance (e.g., `Article.update()` returns new `Article`)
- Private constructor not used; public constructor with all fields
- Authorization predicates as methods (e.g., `canBeDeletedBy(userId)`)

```kotlin
@AggregateRoot
class Article(
    override val id: ArticleId,
    val slug: String,
    val title: String,
    ...
) : Entity<ArticleId>() {
    init {
        require(title.isNotBlank()) { "Title must not be blank" }
    }

    fun update(slug: String, title: String, ...): Article = Article(...)
    fun canBeDeletedBy(userId: UserId): Boolean = userId == authorId
}
```

### Typed IDs
Inline value classes annotated with `@ValueObject`:
```kotlin
@ValueObject
@JvmInline
value class ArticleId(val value: Long)
```

### CQRS Split
- **Write services**: Interface + `Default*` implementation, `@Transactional`, validate input, throw `ValidationException`
- **Read services**: Interface + `Jooq*` implementation, no `@Transactional`, return `*Summary` data classes
- Write services return entity IDs (`Long`), not full entities
- Read services hydrate from ID to summary DTO

### Validation Pattern
Write services collect field-level errors before throwing:
```kotlin
val errors = mutableMapOf<String, List<String>>()
if (title.isBlank()) errors["title"] = listOf("must not be blank")
if (errors.isNotEmpty()) throw ValidationException(errors)
```

### Resource Layer (REST)
- Implement OpenAPI-generated interfaces (e.g., `ArticlesApi`)
- `@ApplicationScoped` — never `@RequestScoped`
- Use `SecurityContext` for auth (injected, `@RequestScoped`)
- Map application-layer summaries to OpenAPI DTOs via private extension functions (`toDto()`)
- OpenAPI models use fluent setters (protected constructors): `ApiArticle().slug("x").title("y")`
- `@RolesAllowed("user")` for authenticated endpoints
- `@ResponseStatus(201)` / `@ResponseStatus(204)` for non-200 success codes

### Repository Pattern
- Interface in `domain/` extends `Repository<T, ID>` (defines `nextId()`, `create()`, `update()`, `findById()`)
- Additional methods per aggregate (e.g., `findBySlug()`, `deleteById()`, `favorite()`)
- Implementation in `infrastructure/` uses jOOQ `DSLContext`
- `multiset()` for eager-loading nested collections (no N+1)
- Batch operations for tag management

### Error Handling
Domain exceptions → JAX-RS ExceptionMapper → HTTP status:
| Exception | HTTP | Usage |
|-----------|------|-------|
| `ValidationException` | 422 | Field-level validation errors |
| `NotFoundException` | 404 | Entity not found |
| `ForbiddenException` | 403 | Authorization failure |
| `UnauthorizedException` | 401 | Missing authentication |
| `BadRequestException` | 400 | Malformed request |
| Unhandled | 500 | Catch-all via `UnhandledExceptionMapper` |

### Dependency Injection
- `@ApplicationScoped` for all services, repositories, resources
- `@RequestScoped` only for `SecurityContext` (extracts JWT per-request)
- Constructor injection (Kotlin primary constructor)
- `@Inject lateinit var` for field injection in test base classes

### Immutability
- `val` over `var` everywhere
- Immutable collections (`Set<String>` for tags)
- Entity mutation returns new instance

## Code Style

- No comments unless warning about hacks or non-obvious business logic
- Backtick method names in tests: `` `should create valid article` ``
- ktlint enforced formatting — run `gradle ktlintFormat` to auto-fix
- Trailing commas in parameter lists
- Named arguments for clarity in service calls and constructors

---

*Conventions audit: 2026-03-13*
