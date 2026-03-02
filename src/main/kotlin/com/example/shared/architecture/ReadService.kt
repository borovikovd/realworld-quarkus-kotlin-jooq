package com.example.shared.architecture

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Stereotype

/**
 * Marks a class as a Read Service (read side) in CQRS.
 *
 * Read services handle read-only operations: they fetch data, map domain/database results
 * to API DTOs, and return responses. They do not modify state and do not need `@Transactional`.
 *
 * ## Conventions
 * - Named `*ReadService` (e.g., `UserReadService`, `ArticleReadService`)
 * - May access jOOQ `DSLContext` directly for optimized read queries
 * - May use OpenAPI-generated DTOs for response mapping
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
