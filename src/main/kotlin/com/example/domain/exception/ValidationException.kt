package com.example.domain.exception

class ValidationException(
    val errors: Map<String, List<String>>,
) : RuntimeException("Validation failed")
