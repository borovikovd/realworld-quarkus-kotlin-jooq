package com.example.common.web

class Validation {
    private val errors = linkedMapOf<String, MutableList<String>>()

    inline fun <T> parse(
        field: String,
        block: () -> T,
    ): Result<T> = runCatching(block).onFailure { add(field, it.message ?: "is invalid") }

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
