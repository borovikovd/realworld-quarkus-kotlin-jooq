package com.example.domain

/**
 * Marks a class as a Value Object in DDD.
 *
 * Value objects are defined by their attributes, not by identity. Two value objects with the
 * same attributes are considered equal. They should be immutable.
 *
 * Typical uses: typed IDs (`UserId`, `ArticleId`), money, email addresses, date ranges.
 *
 * In Kotlin, use `@JvmInline value class` for zero-cost wrappers:
 *
 * ```kotlin
 * @ValueObject
 * @JvmInline
 * value class UserId(val value: Long)
 * ```
 *
 * ## Validation
 *
 * Value objects validate their own construction — an invalid value object should be impossible
 * to create. Use `init` blocks with `require()`:
 *
 * ```kotlin
 * @JvmInline
 * value class Email(val value: String) {
 *     init {
 *         require(value.matches(EMAIL_REGEX)) { "Invalid email format" }
 *     }
 * }
 * ```
 *
 * Like [AggregateRoot] invariants, these throw [IllegalArgumentException] and are a safety net.
 * User-facing input validation belongs in Service.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ValueObject
