package com.example.domain.auth

import com.example.domain.user.Email
import com.example.domain.user.UserId
import com.example.domain.user.Username

interface TokenIssuer {
    fun issue(
        userId: UserId,
        email: Email,
        username: Username,
    ): String
}
