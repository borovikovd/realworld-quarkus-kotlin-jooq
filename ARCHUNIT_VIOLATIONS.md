# ArchUnit Key Violations Summary

**Status:** 9 tests failing out of 24 total tests

---

## Category Breakdown

### ✅ PASSING (15 tests)

**Aggregate Boundaries (5/5)** ✅
- Domain entities don't cross aggregate boundaries
- Services can coordinate aggregates (Application Service pattern)

**Naming Conventions (4/6)** ✅
- Resources, Services, Exceptions, ExceptionMappers - all correct

**Scope & Transactions (3/5)** ✅
- SecurityContext is @RequestScoped
- Services are @ApplicationScoped
- Only Services have @Transactional methods

**Technology Boundaries (3/4)** ✅
- JWT isolated to SecurityContext/JwtService
- jOOQ DSLContext injection isolated to Jooq* classes
- Services don't use JAX-RS Response

---

## ❌ FAILING (9 tests)

### 1. Naming Convention Violations (2 failures)

**Issue:** Rules are not finding any classes to check

#### Repository Naming Rule ❌
```
Rule: implementations of Repository interfaces should be named Jooq*Repository
Status: No classes found (rule not matching correctly)
```

**Actual files (all correct):**
- ✅ `JooqArticleRepository.kt`
- ✅ `JooqCommentRepository.kt`
- ✅ `JooqUserRepository.kt`
- ✅ `JooqFollowRepository.kt`

#### Query Naming Rule ❌
```
Rule: implementations of Queries interfaces should be named Jooq*Queries
Status: No classes found (rule not matching correctly)
```

**Actual files (all correct):**
- ✅ `JooqArticleQueries.kt`
- ✅ `JooqCommentQueries.kt`
- ✅ `JooqProfileQueries.kt`

**Root Cause:** ArchUnit rule syntax issue - `.implement("..Repository")` not matching Kotlin interface implementations properly.

**Fix Required:** Update rule to use `.areAssignableTo()` or check by name pattern instead.

---

### 2. Layer Dependency Violations (4 failures)

#### A. Resources accessing non-Service/Query classes ❌
```
Violation: Resources may be accessing repositories or other classes directly
```

**Likely violations:**
- Resources might inject domain exceptions
- Resources might import utility classes
- Resources might access jOOQ generated code for testing

#### B. Services accessing Queries ❌
```
Violation: Services should only use Repositories, not Queries
Rule: Services are for commands (write), Queries are for reads (CQRS-lite)
```

**Potential violations:**
- Services might be injecting Queries for read operations
- Services might be using Queries to check existence before mutations

#### C. Domain entities not framework-independent ❌
```
Violation: Domain entities (Article, User, Comment) depend on framework code
```

**Potential violations:**
- Domain entities might use Jakarta annotations (@Entity, @Id, etc.)
- Domain entities might use jOOQ types
- Domain entities might use Quarkus utilities

**Note:** In this codebase, domain entities are POJOs without JPA annotations, but they might import:
- `com.example.shared.domain.Entity` interface
- Value objects that use `@JvmInline`
- Kotlin stdlib (which is allowed)

#### D. Non-Jooq classes importing jOOQ generated code ❌
```
Violation: Classes outside Jooq*Repository/Jooq*Queries access com.example.jooq.*
```

**Likely violations:**
- `BaseApiTest` might inject `DSLContext` for test cleanup
- Test fixtures might use jOOQ tables for assertions
- Query builders might be in non-Jooq classes

---

### 3. Scope & Transaction Violations (2 failures)

#### A. Repositories have @Transactional ❌
```
Rule: Repositories should NOT be @Transactional (Services manage transactions)
Status: Some repository might have @Transactional annotation
```

**Check:** Look for `@Transactional` on:
- JooqArticleRepository
- JooqCommentRepository
- JooqUserRepository
- JooqFollowRepository

#### B. Queries have @Transactional ❌
```
Rule: Query classes should NOT be @Transactional (read-only, no tx needed)
Status: Some query class might have @Transactional annotation
```

**Check:** Look for `@Transactional` on:
- JooqArticleQueries
- JooqCommentQueries
- JooqProfileQueries

---

### 4. Technology Boundary Violations (1 failure)

#### OpenAPI DTOs accessed outside Resources ❌
```
Rule: Only Resources should import com.example.api.* (OpenAPI generated DTOs)
Violation: Services or domain classes might be using OpenAPI DTOs
```

**Likely violations:**
- `ApiTestFixtures` (test helper) uses OpenAPI DTOs
- `TestDataBuilder` (test helper) uses OpenAPI DTOs
- Query implementations return OpenAPI DTOs (this is actually correct per CQRS-lite)

**Decision needed:** Should Queries be allowed to return OpenAPI DTOs?
- **Current pattern:** Queries return DTOs, Services work with domain entities
- **Option 1:** Allow Queries to use OpenAPI DTOs (makes sense for CQRS)
- **Option 2:** Strict - only Resources use DTOs, Queries return custom read models

---

## Recommended Fixes (Priority Order)

### Priority 1: Fix Rule Syntax (No Code Changes)
1. **Fix Naming Convention rules** - Update `.implement()` to `.areAssignableTo()` or use name matching
2. **Fix Layer Dependency rules** - Add exclusions for test classes

### Priority 2: Adjust Rules to Match Actual Architecture
3. **Allow Queries to return OpenAPI DTOs** - This is the CQRS-lite pattern we're using
4. **Allow BaseApiTest to inject DSLContext** - Tests need this for cleanup
5. **Clarify domain entity framework independence** - Define what "framework" means (Jakarta? jOOQ? Kotlin?)

### Priority 3: Fix Actual Code Violations (if any)
6. **Remove @Transactional from Repositories/Queries** - If any exist
7. **Move OpenAPI imports from Services to Resources** - If Services use DTOs

---

## Quick Diagnostic Commands

```bash
# Find @Transactional on Repositories
grep -r "@Transactional" src/main/kotlin/*/Jooq*Repository.kt

# Find @Transactional on Queries
grep -r "@Transactional" src/main/kotlin/*/Jooq*Queries.kt

# Find OpenAPI imports in Services
grep -r "com.example.api" src/main/kotlin/*/*Service.kt

# Find jOOQ imports outside Jooq* classes
grep -r "com.example.jooq" src/main/kotlin/ | grep -v "Jooq" | grep -v ".class"
```

---

## Decision Points

**Question 1:** Should Queries be allowed to return OpenAPI DTOs?
- Current: Yes (CQRS-lite pattern - Queries return read models)
- Strict DDD: No (create separate read model DTOs)

**Question 2:** Should test classes be excluded from all rules?
- Current: Some rules exclude tests, some don't
- Recommended: Add consistent test exclusions

**Question 3:** What does "framework independent" mean for domain entities?
- Allow: Kotlin stdlib, basic Java types, shared domain interfaces
- Disallow: Jakarta annotations, jOOQ types, Quarkus specifics
- Gray area: `@JvmInline` value classes?

**Question 4:** Should BaseApiTest be allowed to inject DSLContext?
- Current: Rule blocks it
- Reality: Tests need it for database cleanup
- Fix: Add test exclusion to DSLContext injection rule
