package com.example.shared.architecture

/**
 * Marks a domain entity as an Aggregate Root in DDD.
 *
 * The root is the only member of the AGGREGATE that outside objects are allowed to hold references to.
 * Aggregate roots are entry points for all operations on the aggregate.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AggregateRoot
