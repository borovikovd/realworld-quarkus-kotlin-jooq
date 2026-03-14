# Testing

**Analysis Date:** 2026-03-13

## Framework & Dependencies

- **Test framework:** JUnit 5 (via Quarkus)
- **Mocking:** MockK
- **API testing:** REST Assured
- **Containers:** Testcontainers 2.0.3 (PostgreSQL 17-alpine)
- **Architecture testing:** ArchUnit
- **Quarkus integration:** `@QuarkusTest` + `@QuarkusTestResource`

## Test Structure

```
src/test/kotlin/com/example/
├── archunit/                          # Architecture fitness tests
│   ├── ArchitectureTest.kt            # Base class with @AnalyzeClasses
│   ├── AggregateBoundaryRules.kt      # Cross-aggregate boundary rules
│   ├── LayerDependencyRules.kt        # Layer dependency enforcement
│   ├── NamingConventionRules.kt       # Class naming rules
│   ├── ScopeAndTransactionRules.kt    # DI scope and @Transactional rules
│   └── TechnologyBoundaryRules.kt     # Framework dependency rules
├── article/
│   ├── domain/ArticleTest.kt          # Entity invariant tests
│   ├── application/DefaultArticleWriteServiceTest.kt  # Service unit tests
│   └── infrastructure/ArticleApiTest.kt  # API integration tests
├── comment/
│   ├── domain/CommentTest.kt
│   └── application/DefaultCommentWriteServiceTest.kt
├── profile/
│   └── application/DefaultProfileWriteServiceTest.kt
├── user/
│   ├── domain/UserTest.kt
│   ├── application/DefaultUserWriteServiceTest.kt
│   └── infrastructure/UserAuthApiTest.kt
└── shared/
    ├── BaseApiTest.kt                 # Base class for API tests (DB cleanup)
    ├── ApiTestFixtures.kt             # Helper methods for common API operations
    ├── TestDataBuilder.kt             # Test data generation with unique values
    ├── PostgresTestResource.kt        # Testcontainers PostgreSQL lifecycle
    └── security/
        ├── PasswordHasherTest.kt      # Unit test
        └── RateLimiterTest.kt         # Unit test
```

## Test Types

### 1. Domain Unit Tests (`{agg}/domain/{Entity}Test.kt`)
Pure unit tests for domain entity behavior. No mocks, no framework.

**What they test:**
- Constructor invariants (`require()` blocks) — blank field rejection
- Entity mutation methods (e.g., `Article.update()`)
- Authorization predicates (e.g., `canBeDeletedBy()`)
- Identity-based equality

**Pattern:**
```kotlin
class ArticleTest {
    @Test
    fun `should fail when title is blank`() {
        assertThrows<IllegalArgumentException> {
            Article(id = ArticleId(1L), slug = "s", title = "", ...)
        }
    }
}
```

### 2. Service Unit Tests (`{agg}/application/Default{Entity}WriteServiceTest.kt`)
Unit tests for write service logic using MockK.

**What they test:**
- Input validation (ValidationException with field errors)
- Business logic (authorization, slug generation, CRUD orchestration)
- Repository interaction (verify correct calls)
- Error cases (NotFoundException, ForbiddenException)

**Pattern:**
```kotlin
class DefaultArticleWriteServiceTest {
    private lateinit var articleRepository: ArticleRepository
    private lateinit var slugGenerator: SlugGenerator
    private lateinit var securityContext: SecurityContext

    @BeforeEach
    fun setup() {
        articleRepository = mockk()
        slugGenerator = mockk()
        securityContext = mockk()
        articleWriteService = DefaultArticleWriteService(
            articleRepository = articleRepository,
            slugGenerator = slugGenerator,
            securityContext = securityContext,
        )
    }

    @Test
    fun `createArticle should throw ValidationException when title is blank`() {
        val exception = assertThrows<ValidationException> {
            articleWriteService.createArticle("", "desc", "body", emptyList())
        }
        assertEquals(listOf("must not be blank"), exception.errors["title"])
    }
}
```

**Mocking conventions:**
- `mockk()` for creating mocks (no relaxed mocks)
- `every { ... } returns ...` for stubbing
- `every { ... } answers { firstArg() }` for pass-through repository saves
- `verify { ... }` for interaction verification
- `verify(exactly = 0) { ... }` for negative verification

### 3. API Integration Tests (`{agg}/infrastructure/{Entity}ApiTest.kt`)
Full-stack integration tests with real PostgreSQL via Testcontainers.

**What they test:**
- HTTP status codes for success and error cases
- Response body structure and values
- Authentication and authorization flows
- End-to-end CRUD operations

**Pattern:**
```kotlin
@QuarkusTest
class ArticleApiTest : BaseApiTest() {
    @Test
    fun `should create article`() {
        val user = ApiTestFixtures.registerUser()
        val title = TestDataBuilder.uniqueTitle()

        ApiTestFixtures.authenticatedRequest(user.token)
            .body(TestDataBuilder.articleCreation(title = title))
            .`when`()
            .post("/api/articles")
            .then()
            .statusCode(201)
            .body("article.title", equalTo(title))
    }
}
```

### 4. Architecture Fitness Tests (`archunit/`)
ArchUnit rules enforcing architectural constraints at compile time.

**Rule categories:**
- `NamingConventionRules` — class naming patterns (e.g., repos must be `Jooq*Repository`)
- `LayerDependencyRules` — layer dependency direction (domain has no infrastructure imports)
- `AggregateBoundaryRules` — cross-aggregate boundary enforcement
- `ScopeAndTransactionRules` — correct `@ApplicationScoped`/`@RequestScoped` usage, `@Transactional` placement
- `TechnologyBoundaryRules` — framework dependencies restricted to infrastructure layer

**Pattern:**
```kotlin
class NamingConventionRules : ArchitectureTest() {
    @ArchTest
    val `repository implementations should be named Jooq-Repository` =
        classes()
            .that().implement(Repository::class.java)
            .and().areNotInterfaces()
            .should().haveSimpleNameStartingWith("Jooq")
}
```

### 5. Utility Unit Tests (`shared/security/`)
Unit tests for shared utilities: `PasswordHasherTest`, `RateLimiterTest`, `SlugGeneratorTest`.

## Test Infrastructure

### PostgresTestResource (`shared/PostgresTestResource.kt`)
Testcontainers lifecycle manager for integration tests:
- Starts PostgreSQL 17-alpine container
- Applies initial migration SQL (`db/migrations/20251013181033_initial.sql`)
- Provides JDBC URL, username, password to Quarkus config
- Container shared across all `@QuarkusTest` classes via `@QuarkusTestResource`

### BaseApiTest (`shared/BaseApiTest.kt`)
Abstract base class for API integration tests:
- Injects `DSLContext` for direct DB access
- `@BeforeEach` truncates all public tables with `RESTART IDENTITY CASCADE`
- Ensures test isolation — every test starts with clean database

### ApiTestFixtures (`shared/ApiTestFixtures.kt`)
Static helper methods for common API test operations:
- `registerUser()` → creates user, returns `UserRegistrationResult` (email, username, password, token)
- `loginUser()` → authenticates, returns token
- `createArticle()` → creates article, returns `ArticleCreationResult` (slug, title)
- `createComment()` → creates comment, returns `CommentCreationResult` (id, body)
- `authenticatedRequest()` → REST Assured spec with Bearer token

### TestDataBuilder (`shared/TestDataBuilder.kt`)
Generates unique test data using UUID prefixes:
- `uniqueEmail()`, `uniqueUsername()`, `uniqueTitle()` — avoid collisions between tests
- `userRegistration()`, `userLogin()`, `articleCreation()`, etc. — JSON request bodies as strings

## Running Tests

```bash
# All tests
gradle test

# Specific test class
gradle test --tests DefaultArticleWriteServiceTest

# Specific test method (use full qualified class)
gradle test --tests "com.example.article.domain.ArticleTest.should create valid article"

# Architecture tests only
gradle test --tests "com.example.archunit.*"

# Full build (includes tests + linting)
gradle build
```

## Coverage Gaps

- No API tests for `TagResource`, `FavoriteResource`
- No integration tests for read services (`JooqProfileReadService`, `JooqCommentReadService`)
- No concurrent/race condition tests
- No performance/load tests

---

*Testing audit: 2026-03-13*
