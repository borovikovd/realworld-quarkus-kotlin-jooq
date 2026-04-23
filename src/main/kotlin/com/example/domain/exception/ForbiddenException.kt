package com.example.domain.exception

class ForbiddenException(
    message: String = "Forbidden",
) : RuntimeException(message)
