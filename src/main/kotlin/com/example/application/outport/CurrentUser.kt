package com.example.application.outport

import com.example.domain.aggregate.user.UserId
import com.example.domain.exception.UnauthorizedException
import java.util.UUID

interface CurrentUser {
    val id: UserId?
    val jti: UUID?

    fun require(): UserId = id ?: throw UnauthorizedException("Authentication required")
}
