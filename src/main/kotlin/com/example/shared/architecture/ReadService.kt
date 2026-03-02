package com.example.shared.architecture

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Stereotype

/**
 * Marks a class as a Read Service (read side) in CQRS.
 *
 * Read services handle read-only operations: they fetch data, map database results
 * to read-side data classes, and return them. They do not modify state and do not need `@Transactional`.
 *
 * ## Conventions
 * - Named `*ReadService` (e.g., `UserReadService`, `ArticleReadService`)
 * - May access jOOQ `DSLContext` directly for optimized read queries
 * - Return plain Kotlin data classes (e.g., `ArticleSummary`, `UserSummary`), not OpenAPI DTOs
 * - No `@Transactional` — connection pooling handles read concurrency
 *
 * ## Validation
 *
 * Read services perform **no input validation**. They receive already-validated IDs or
 * parameters from the Resource layer and simply return data or null/empty results.
 */
@Stereotype
@ApplicationScoped
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ReadService
