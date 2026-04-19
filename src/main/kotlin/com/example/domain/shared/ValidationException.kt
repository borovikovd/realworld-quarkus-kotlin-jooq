package com.example.domain.shared

class ValidationException(
    val errors: Map<String, List<String>>,
) : RuntimeException("Validation failed")
