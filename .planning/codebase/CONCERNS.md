# Codebase Concerns

**Analysis Date:** 2026-03-13

## Tech Debt

**Unsafe Type Casting in Article Queries:**
- Issue: Multiple `@Suppress("UNCHECKED_CAST")` annotations used to cast jOOQ multiset results to `List<String>` without runtime verification
- Files: `src/main/kotlin/com/example/article/infrastructure/JooqArticleRepository.kt` (line 154), `src/main/kotlin/com/example/article/infrastructure/JooqArticleReadService.kt` (line 229), `src/main/kotlin/com/example/article/infrastructure/ArticleResource.kt` (lines 57, 82)
- Impact: Silent failures if jOOQ result structure doesn't match expectations; could return `null` instead of list, caught only at runtime
- Fix approach: Create a strongly-typed wrapper around multiset results or add explicit null checks before casting

**Slug Generation Race Condition:**
- Issue: `slugGenerator.generateUniqueSlug()` checks existence then writes in separate transactions; another request can insert same slug between check and write
- Files: `src/main/kotlin/com/example/article/application/DefaultArticleWriteService.kt` (lines 37-43, 80-91)
- Impact: Duplicate slug creation when articles created concurrently, violating unique constraint in database
- Fix approach: Rely on database unique constraint to catch duplicates; catch constraint violation exception and retry with modified slug, or use database-level slug generation

**Integer Overflow Risk in Favorites Count:**
- Issue: Favorites count stored/queried as `Int` which can overflow at 2.1 billion records per article
- Files: `src/main/kotlin/com/example/article/infrastructure/JooqArticleReadService.kt` (lines 170-178, 213-216)
- Impact: Counter wraparound causing incorrect favorite counts; very low probability at current scale but architectural debt
- Fix approach: Change to `Long` in schema and query results

## Known Bugs

**Rate Limiter Memory Leak:**
- Symptoms: RateLimiter cache can grow unbounded; with 10,000 unique IPs making requests, all stay in cache until window expires
- Files: `src/main/kotlin/com/example/shared/security/RateLimiter.kt` (lines 16-21)
- Trigger: Many unique IPs hitting rate-limited endpoints (e.g., brute-force attack against login)
- Workaround: None; restart application
- Fix approach: Add expiration monitoring or limit cache size more aggressively based on memory usage

## Security Considerations

**CORS Allow-All Configuration:**
- Risk: `quarkus.http.cors.origins=*` allows cross-origin requests from any domain; combined with JWT auth could expose users to token theft via malicious sites
- Files: `src/main/resources/application.properties` (line 30)
- Current mitigation: JWT tokens required for mutations; GET requests can be made but no sensitive data beyond article/comment body exposed
- Recommendations: Configure allowed origins to specific domains for production (`https://frontend.example.com`); evaluate if CORS needed at all for backend API

**JWT Token Expiry at Boundary:**
- Risk: 60-day token expiry is long; if token leaked, attacker has 60 days of access
- Files: `src/main/kotlin/com/example/shared/security/JwtService.kt` (line 25)
- Current mitigation: No refresh token mechanism; user must re-login after expiry
- Recommendations: Implement shorter-lived access tokens (15 minutes) with refresh token rotation for production

**Rate Limiting IP Address Extraction:**
- Risk: `X-Forwarded-For` header can be spoofed behind non-trustworthy proxies
- Files: `src/main/kotlin/com/example/shared/security/RateLimitFilter.kt` (lines 54-61)
- Current mitigation: Fallback to `X-Real-IP` then "unknown"; generic rate limiting still applied
- Recommendations: Configure trusted proxy IP ranges in Quarkus; validate proxy headers against whitelist; consider per-user rate limiting for authenticated endpoints

## Performance Bottlenecks

**N+1 Query Potential in Slug Uniqueness Check:**
- Problem: Each article creation/update calls `articleRepository.findBySlug()` to check uniqueness during slug generation, then again in write service before update
- Files: `src/main/kotlin/com/example/article/application/DefaultArticleWriteService.kt` (lines 37-43, 84-86)
- Cause: Separation of slug generation concern from write logic; checking before inserting
- Improvement path: Use database unique constraint violation as source of truth; avoid pre-check query; handle `IntegrityConstraintViolationException` in repository

**Count Queries Without Filtering Optimization:**
- Problem: `countArticles()` and `countArticlesFeed()` run separate SELECT COUNT(*) even when filtering by tag/author/favorited
- Files: `src/main/kotlin/com/example/article/infrastructure/JooqArticleReadService.kt` (lines 144-165)
- Cause: Count queries don't use indexes effectively on multiple filter combinations; no query plan caching
- Improvement path: Add database-level query result caching for popular tag/author combinations; consider materialized views for hot queries

**Multiset Subqueries for Every Article Field:**
- Problem: Every article fetch runs subqueries for favorite count, following status, and tag list even if not needed
- Files: `src/main/kotlin/com/example/article/infrastructure/JooqArticleReadService.kt` (lines 167-219)
- Cause: Article resource always returns complete ArticleSummary with all fields
- Improvement path: Implement sparse field selection or GraphQL to fetch only requested fields

## Fragile Areas

**Type-Safe Multiset Casting:**
- Files: `src/main/kotlin/com/example/article/infrastructure/JooqArticleReadService.kt` (lines 206-212), `JooqArticleRepository.kt` (lines 113-121, 135-143)
- Why fragile: jOOQ multiset fields are untyped until cast; wrong field order or missing fields cause runtime `ClassCastException`
- Safe modification: Add integration tests verifying exact field order matches casts; create helper methods extracting and validating multiset structure
- Test coverage: Only ArticleApiTest covers multiset; tag fetching not unit-tested in isolation

**Slug Generation Uniqueness:**
- Files: `src/main/kotlin/com/example/article/application/DefaultArticleWriteService.kt` (lines 37-43, 80-91)
- Why fragile: Synchronous check-then-act pattern; concurrent requests can bypass slug uniqueness despite check
- Safe modification: Always catch `IntegrityConstraintViolationException` on insert; retry with slug suffix; never rely on pre-check
- Test coverage: No concurrent test for slug uniqueness; only single-threaded happy path tested

## Scaling Limits

**Pagination Without Cursor:**
- Current capacity: Offset pagination works well up to millions of records
- Limit: Offset scanning becomes O(n) slow when offset > 1M; users cannot efficiently paginate near end of large result sets
- Scaling path: Implement cursor-based pagination using unique, indexed `created_at` column; maintain backward compatibility with offset via translation layer

**Rate Limiter Memory Unbounded:**
- Current capacity: Caffeine cache max 10,000 entries with 1-minute window = up to 10K unique IPs tracked
- Limit: Grows to 10GB+ under coordinated attack with 1M unique IPs; can cause OOM and service restart
- Scaling path: Implement sliding window rate limiter with fixed memory footprint; use Redis/Memcached for distributed rate limiting in multi-instance deployment

## Dependencies at Risk

**Testcontainers Pinned to 2.0.3:**
- Risk: Latest is 2.1.0+; pinning is due to Quarkus 3.32 BOM drop compatibility issue per git commit `1f2723f`
- Impact: Missing bug fixes and security patches in Testcontainers; test environment diverges from latest
- Migration plan: Track Quarkus BOM updates; upgrade when compatible version available; add test coverage for JVM version compatibility

## Test Coverage Gaps

**Resource/API Layer Integration Gaps:**
- Untested: `TagResource.kt`, `FavoriteResource.kt` (these files exist but no corresponding test files found)
- Files: `src/main/kotlin/com/example/article/infrastructure/FavoriteResource.kt`, `src/main/kotlin/com/example/article/infrastructure/TagResource.kt`
- Risk: Favorite and tag operations may fail in production undetected; breaking API changes not caught at compile time
- Priority: **High** - Public API endpoints without test coverage

**Read Service Integration Tests Missing:**
- Untested: `JooqProfileReadService.kt`, `JooqCommentReadService.kt` fetching logic; only write services tested
- Files: `src/main/kotlin/com/example/profile/infrastructure/JooqProfileReadService.kt`, `src/main/kotlin/com/example/comment/infrastructure/JooqCommentReadService.kt`
- Risk: Query bugs (missing joins, wrong field projections) only discovered in production
- Priority: **High** - Database query logic without testing

**Concurrent Modification Scenarios:**
- Untested: Race conditions in article updates (two users editing same article); concurrent slug generation
- Risk: Concurrent requests may succeed when they should fail or cause data corruption
- Priority: **Medium** - Unlikely in typical usage but possible under concurrent access

**Rate Limiter Distributed Behavior:**
- Untested: RateLimiter behavior across multiple application instances
- Risk: Rate limiting per-IP not working if app deployed with multiple instances behind load balancer
- Priority: **High** for scaled deployment

---

*Concerns audit: 2026-03-13*
