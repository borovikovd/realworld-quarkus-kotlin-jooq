package com.example.domain.shared

/**
 * Marks a domain entity as an Aggregate Root in DDD.
 *
 * The root is the only member of the AGGREGATE that outside objects are allowed to hold references to.
 * Aggregate roots are entry points for all operations on the aggregate.
 *
 * ## Validation
 *
 * Aggregate roots enforce **domain invariants** — rules that must always hold regardless of how the
 * object is constructed. Use `require()` in `init` blocks:
 *
 * ```kotlin
 * init {
 *     require(title.isNotBlank()) { "Title must not be blank" }
 * }
 * ```
 *
 * These are a **safety net**, not the primary validation path. They throw [IllegalArgumentException]
 * and signal a programming error (the caller should have validated first). User-facing input
 * validation with field-level error messages belongs in [WriteService].
 *
 * ## What belongs here
 * - Structural invariants: non-blank fields, format rules, length bounds
 * - Cross-field invariants: "end date must be after start date"
 * - Authorization predicates: `canBeDeletedBy(userId)`
 *
 * ## What does NOT belong here
 * - Uniqueness checks (require repository access)
 * - Password strength rules (not a property of the entity)
 * - Any validation that depends on external state
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AggregateRoot
