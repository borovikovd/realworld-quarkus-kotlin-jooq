package com.example.application.outport.security

import com.example.application.readmodel.IssuedTokens
import com.example.domain.aggregate.user.UserId
import java.time.Duration

interface TokenIssuer {
    fun issue(userId: UserId): IssuedTokens

    fun issueAccessToken(userId: UserId): String

    fun accessTokenExpiry(): Duration
}
