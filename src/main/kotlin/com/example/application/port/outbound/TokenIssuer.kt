package com.example.application.port.outbound

import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.UserId
import com.example.domain.aggregate.user.Username

interface TokenIssuer {
    fun issue(
        userId: UserId,
        email: Email,
        username: Username,
    ): String
}
