package com.example.domain.shared

class ForbiddenException(
    message: String = "Forbidden",
) : RuntimeException(message)
