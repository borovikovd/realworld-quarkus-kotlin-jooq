package com.example.application.port.security

import com.example.domain.aggregate.user.UserId
import com.example.domain.exception.UnauthorizedException
import java.util.UUID

interface CurrentUser {
    val id: UserId?
    val jti: UUID?
    val rawToken: String?

    fun require(): UserId = id ?: throw UnauthorizedException("Authentication required")
}
