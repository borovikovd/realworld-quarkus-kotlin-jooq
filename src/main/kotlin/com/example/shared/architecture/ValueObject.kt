package com.example.shared.architecture

/**
 * Marks a value object in DDD.
 *
 * Value objects are immutable and defined by their attributes rather than identity.
 * They should be implemented as Kotlin data classes.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ValueObject
