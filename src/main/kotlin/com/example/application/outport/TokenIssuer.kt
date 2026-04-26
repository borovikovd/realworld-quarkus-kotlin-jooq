package com.example.application.outport

import com.example.domain.aggregate.user.UserId

interface TokenIssuer {
    fun issue(userId: UserId): String
}
