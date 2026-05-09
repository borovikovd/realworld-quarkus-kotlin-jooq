package com.example.common.web

class NotFoundException(
    val field: String,
    message: String,
) : RuntimeException(message)

class ForbiddenException(
    val field: String,
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

class InvalidCredentialsException : RuntimeException("Invalid credentials")
