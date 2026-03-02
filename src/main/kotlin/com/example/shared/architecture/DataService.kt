package com.example.shared.architecture

/**
 * Marks a class as a Data Service (read side) in CQRS.
 *
 * Data services handle read-only operations: they fetch data, map domain/database results
 * to API DTOs, and return responses. They do not modify state and do not need `@Transactional`.
 *
 * ## Conventions
 * - Named `*DataService` (e.g., `UserDataService`, `ArticleDataService`)
 * - May access jOOQ `DSLContext` directly for optimized read queries
 * - May use OpenAPI-generated DTOs for response mapping
 * - No `@Transactional` — connection pooling handles read concurrency
 *
 * ## Validation
 *
 * Data services perform **no input validation**. They receive already-validated IDs or
 * parameters from the Resource layer and simply return data or null/empty results.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DataService
