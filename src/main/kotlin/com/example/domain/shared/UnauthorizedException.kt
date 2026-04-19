package com.example.domain.shared

class UnauthorizedException(
    message: String = "Unauthorized",
) : RuntimeException(message)
