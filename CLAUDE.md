# Claude Code Assistant Guidelines

**Focus:** How to write code, tool usage, common gotchas

## Core Principles

**Production-Grade Code**
- Write idiomatic, maintainable code over quick shortcuts
- Use proper patterns from the start (interfaces, abstractions)
- Avoid technical debt, but don't over-engineer
- NO comments unless warning about hacks or non-obvious business logic
- Use structured logging (ERROR/WARN/INFO), never log PII

**Development Workflow**
1. Write code + tests together
2. Compile (`gradle compileKotlin`)
3. Run focused tests (specific test class)
4. Run related tests (package/aggregate)
5. Full build verification (`gradle build` - includes compilation, tests, and linting)
6. Auto-fix code style if needed (`gradle ktlintFormat`)

## Dependency Management

**ALWAYS before adding ANY dependency:**
1. WebSearch latest version on Maven Central
2. Read official docs for current API usage
3. Verify Quarkus compatibility
4. Prefer Quarkus extensions when available
5. Add brief comment only if version choice is non-obvious

## Kotlin Best Practices

**Data Structures**
- Use `data class` for DTOs, POJOs (auto equals/hashCode/copy)
- Use regular `class` for entities with behavior
- Use `object` for singletons
- Domain entities: private constructor + `companion object` with factory methods

**Null Safety**
- Leverage Kotlin's null safety (`?`, `?.`, `?:`, `!!`)
- Use `!!` only when certain value exists
- Prefer `?:` with default values over `!!`

**Scope Functions**
- `let` - transform nullable to non-null
- `apply` - configure object
- `also` - side effects
- `run` - compute result from object

**Immutability**
- Prefer `val` over `var`
- Use immutable collections where possible
- Make entity fields private with controlled mutation methods

**Collections**
- Use `listOf()`, `setOf()`, `mapOf()` for immutable
- Use `mutableListOf()` etc. only when mutation needed
- Prefer `map`, `filter`, `fold` over imperative loops

## Coding Patterns

**Repositories**
- Interface: `ArticleRepository`, Implementation: `JooqArticleRepository`
- Methods work with full aggregates
- Example: `save(article: Article): Article`, `findBySlug(slug: String): Article?`
- Repository handles all child entities within aggregate

**Services**
- Use `@Transactional` on command service methods (not repositories)
- Keep transactions short and focused
- QueryService reads don't need `@Transactional` (connection pooling handles concurrency)
- Service orchestrates, repository persists

**Exception Handling**
- Throw domain exceptions: `NotFoundException`, `ValidationException`, `ForbiddenException`
- Let ExceptionMappers handle HTTP status mapping
- ValidationException for 422 with field-level errors

**DTO Usage**
- Map domain entities to DTOs at Resource (API) layer
- Never expose domain entities directly via API
- Keep DTOs in query/ package

## Database & jOOQ

**Atlas Migrations**
- Source: HCL schema (db/schema.hcl), NOT SQL
- Workflow: Edit HCL → `atlas migrate diff <name>` → `atlas migrate apply`
- Regenerate jOOQ after: `gradle generateJooq`
- Never commit generated code (build/generated/)

**jOOQ Query Building**
- Build conditions in list first, then apply: `baseQuery.where(conditions)`
- Don't reassign query variables (type changes after `.where()`)
- Use `multiset()` for nested collections (NO N+1 queries)
- Fetch all related data in single query
- Never query inside map/forEach loops

**jOOQ Type Handling**
- `fetchOne()` returns nullable Record
- `value1()` returns nullable field value
- Always handle nullability: prefer `?:` over `!!`
- Use `!!` only when absolutely certain value exists

**Performance**
- Add indexes on: foreign keys, slugs, frequently filtered columns
- Use pagination with limit/offset
- Verify no N+1 queries (enable query logging in dev)

## Quarkus Specifics

**Dependency Injection**
- `@ApplicationScoped` - Stateless services (singleton)
- `@RequestScoped` - Request-bound data (SecurityContext)
- `@Inject` for field injection (use `lateinit var`)
- Never store request data in ApplicationScoped beans

**OpenAPI Generated Models**
- Models have protected constructors - use fluent setters
- Chain method calls: `.user(User().email("x").token("y"))`
- Each setter returns object for chaining

**Code Generation**
- OpenAPI: `gradle generateApi` (after spec changes)
- jOOQ: `gradle generateJooq` (after schema changes)
- Add generated sources to Kotlin source sets
- Depend on generation tasks in compileKotlin

## Security Checklist

- [ ] Never log passwords, tokens, or PII
- [ ] Parameterized queries (jOOQ handles this)
- [ ] Validate all user input at service layer
- [ ] Authorization checks before mutations
- [ ] Argon2id for password hashing
- [ ] JWT token expiry configured (60 days default)
- [ ] CORS configured for production domains

## Testing

**Test Strategy**
- Many unit tests (domain entities, utilities, pure functions)
- Some integration tests (repositories with Testcontainers, API with @QuarkusTest)
- Few E2E tests (critical flows only)

**Testcontainers Setup**
- `@QuarkusTest` + `@QuarkusTestResource(PostgresResource::class)`
- Test against real PostgreSQL container
- Verify constraints, transactions, cascade deletes
- Test unique violations, foreign key constraints

**Writing Tests**
- Test domain logic in entity methods
- Test repository operations against real DB
- Mock repositories in service tests
- Test API with full Quarkus stack

## Code Quality

**Linting (ktlint)**
- Plugin: `org.jlleitschuh.gradle.ktlint`
- Run `gradle ktlintCheck` before commit
- Auto-fix with `gradle ktlintFormat`
- Configure in build.gradle.kts

**Git Workflow**
- .gitignore: build/, .gradle/, *.class, .idea/, .quarkus/
- Conventional commits: feat/fix/refactor/test/docs
- Imperative mood, < 72 chars first line

## Common Pitfalls

**jOOQ Type Mismatches**
```kotlin
// WRONG - query type changes after .where()
var query = dsl.select().from(ARTICLES)
query = query.where(ARTICLES.SLUG.eq("foo"))  // Type mismatch!

// CORRECT - build conditions first
val conditions = mutableListOf<Condition>()
conditions.add(ARTICLES.SLUG.eq("foo"))
val query = dsl.select().from(ARTICLES).where(conditions)
```

**Null Safety Ignored**
```kotlin
// WRONG - ignoring nullable
val id = dsl.select(USERS.ID).from(USERS).fetchOne().value1()  // NPE risk!

// CORRECT - handle nullability
val id = dsl.select(USERS.ID)
    .from(USERS)
    .fetchOne()
    ?.value1() ?: throw NotFoundException()
```

**DI Scope Confusion**
```kotlin
// WRONG - request data in singleton
@ApplicationScoped
class SomeService {
    var currentUserId: Long? = null  // Shared across requests!
}

// CORRECT - use RequestScoped
@RequestScoped
class SecurityContext {
    val currentUserId: Long?  // New instance per request
}
```

**Generated Code Issues**
```kotlin
// WRONG - trying to use constructor
val response = Login200Response(user = userDto)  // Constructor is protected!

// CORRECT - use fluent setters
val response = Login200Response().user(userDto)
```

**N+1 Query Pattern**
```kotlin
// WRONG - query inside loop
val articles = dsl.select().from(ARTICLES).fetch()
articles.map { article ->
    val tags = dsl.select().from(TAGS)
        .where(TAGS.ARTICLE_ID.eq(article.id))
        .fetch()  // N+1!
}

// CORRECT - use multiset
val articles = dsl.select(
    ARTICLES.asterisk(),
    multiset(
        select(TAGS.NAME).from(TAGS)
            .where(TAGS.ARTICLE_ID.eq(ARTICLES.ID))
    ).`as`("tags")
).from(ARTICLES).fetch()
```

## Quick Commands

```bash
# Generate code
gradle clean generateApi generateJooq

# Dev mode (hot reload)
gradle quarkusDev

# Database migrations
atlas migrate diff add_field --env local
atlas migrate apply --env local
gradle generateJooq

# Testing
gradle test --tests ArticleServiceTest
gradle test

# Code quality
gradle ktlintFormat
gradle ktlintCheck

# Build
gradle build
gradle build -Dquarkus.package.type=native
```

## Pre-Commit Checklist

- [ ] Code compiles (`gradle compileKotlin`)
- [ ] New tests written and passing
- [ ] Related tests pass (package/aggregate)
- [ ] Full build succeeds (`gradle build` - includes tests and linting)
- [ ] No generated code committed (build/, .gradle/)
- [ ] No secrets committed (.env, credentials, etc.)

## Documentation

- Update openapi.yaml first, then generate code
- README.md - Quick start, tech stack, architecture overview
- CLAUDE.md - Development guidelines (THIS FILE)
- Swagger UI auto-generated from OpenAPI spec
