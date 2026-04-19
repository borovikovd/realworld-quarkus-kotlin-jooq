package com.example.domain.shared

class BadRequestException(
    message: String,
) : RuntimeException(message)
