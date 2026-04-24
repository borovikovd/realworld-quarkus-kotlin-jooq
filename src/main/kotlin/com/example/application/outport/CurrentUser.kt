package com.example.application.outport

import com.example.domain.aggregate.user.UserId
import com.example.domain.exception.UnauthorizedException

interface CurrentUser {
    val id: UserId?

    fun require(): UserId = id ?: throw UnauthorizedException("Authentication required")
}
