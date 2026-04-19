package com.example.domain.auth

import com.example.user.domain.Email
import com.example.user.domain.UserId
import com.example.user.domain.Username

interface TokenIssuer {
    fun issue(
        userId: UserId,
        email: Email,
        username: Username,
    ): String
}
