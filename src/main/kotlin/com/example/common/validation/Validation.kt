package com.example.common.validation

class Validation {
    private val errors = linkedMapOf<String, MutableList<String>>()

    fun check(
        field: String,
        condition: Boolean,
        lazyMessage: () -> String,
    ) {
        if (!condition) add(field, lazyMessage())
    }

    fun add(
        field: String,
        message: String,
    ) {
        errors.getOrPut(field) { mutableListOf() } += message
    }

    fun throwIfInvalid() {
        if (errors.isNotEmpty()) throw ValidationException(errors.mapValues { it.value.toList() })
    }
}

class ValidationException(
    val errors: Map<String, List<String>>,
) : RuntimeException("Validation failed")
