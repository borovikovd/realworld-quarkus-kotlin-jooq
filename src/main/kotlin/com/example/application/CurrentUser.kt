package com.example.application

import com.example.domain.shared.UnauthorizedException
import com.example.user.domain.UserId

interface CurrentUser {
    val id: UserId?

    fun require(): UserId = id ?: throw UnauthorizedException("Authentication required")
}
