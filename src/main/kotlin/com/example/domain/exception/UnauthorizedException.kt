package com.example.domain.exception

class UnauthorizedException(
    message: String = "Unauthorized",
) : RuntimeException(message)
