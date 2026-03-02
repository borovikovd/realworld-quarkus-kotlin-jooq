package com.example.shared.architecture

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Stereotype

/**
 * Marks a class as an Application Service (command side) in DDD.
 *
 * Application services orchestrate use cases: they accept user input, validate it, coordinate
 * domain objects and repositories, and manage transactions. They are the entry point for all
 * write operations.
 *
 * ## Validation
 *
 * Application services are the **primary validation layer** for user input. They validate
 * BEFORE constructing domain objects, collect field-level errors, and throw
 * [com.example.shared.exceptions.ValidationException] (HTTP 422):
 *
 * ```kotlin
 * val errors = mutableMapOf<String, List<String>>()
 * if (userRepository.existsByEmail(email)) {
 *     errors["email"] = listOf("is already taken")
 * }
 * if (errors.isNotEmpty()) {
 *     throw ValidationException(errors)
 * }
 * // Safe to construct entity — domain invariants won't fire
 * val user = User(...)
 * ```
 *
 * ## What belongs here
 * - Input validation with user-friendly error messages
 * - Checks requiring external state: uniqueness, existence, authorization
 * - Password strength, business-rule thresholds
 * - Aggregating multiple field errors into one response
 * - Transaction boundaries (`@Transactional`)
 *
 * ## What does NOT belong here
 * - Domain invariants (belong in [AggregateRoot] / Entity `init` blocks)
 * - DTO mapping (belongs in Resource / [DataService] layer)
 * - Direct jOOQ / database access (belongs in Repository implementations)
 *
 * ## Validation layers (defense in depth)
 *
 * | Layer              | Purpose                          | Exception               | HTTP |
 * |--------------------|----------------------------------|-------------------------|------|
 * | ApplicationService | User input → friendly errors     | ValidationException     | 422  |
 * | Entity init block  | Domain invariants (safety net)   | IllegalArgumentException| 500  |
 * | Database           | Structural integrity (last wall) | SQL constraint violation | 500  |
 */
@Stereotype
@ApplicationScoped
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationService
