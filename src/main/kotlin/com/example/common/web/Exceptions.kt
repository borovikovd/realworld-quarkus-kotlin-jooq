package com.example.common.web

class NotFoundException(
    message: String,
) : RuntimeException(message)

class ForbiddenException(
    message: String,
) : RuntimeException(message)

class UnauthorizedException(
    message: String,
) : RuntimeException(message)

class ValidationException(
    val errors: Map<String, List<String>>,
) : RuntimeException("Validation failed")

class ConflictException(
    val errors: Map<String, List<String>>,
) : RuntimeException("Conflict")
