package com.example.application.port.security

import com.example.application.readmodel.IssuedTokens
import com.example.application.readmodel.StoredRefreshToken
import com.example.domain.aggregate.user.UserId
import java.time.Duration
import java.time.OffsetDateTime

interface TokenIssuer {
    fun accessTokenExpiry(): Duration

    /** Issues a full token pair (access + refresh). Use after login, register, or token rotation. */
    fun issueTokens(userId: UserId): IssuedTokens

    fun findRefreshToken(token: String): StoredRefreshToken?

    /** Returns false if the token was already revoked or not found. */
    fun revokeRefreshToken(token: String): Boolean

    fun revokeAllRefreshTokens(userId: UserId)

    fun purgeExpiredRefreshTokens(before: OffsetDateTime): Int
}
