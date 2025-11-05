package com.example.shared.architecture

/**
 * Marks a domain service that contains business logic not naturally belonging to any entity.
 *
 * Domain services should be stateless and focus on domain operations.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DomainService
