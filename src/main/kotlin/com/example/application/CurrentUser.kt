package com.example.application

import com.example.shared.exceptions.UnauthorizedException
import com.example.user.domain.UserId

interface CurrentUser {
    val id: UserId?

    fun require(): UserId = id ?: throw UnauthorizedException("Authentication required")
}
