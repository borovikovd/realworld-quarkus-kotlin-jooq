package com.example.common.security

import com.example.user.UserId
import java.time.OffsetDateTime

@JvmInline
value class PasswordHash(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Password hash must not be blank" }
    }

    override fun toString(): String = "PasswordHash(***)"
}

data class IssuedTokens(
    val accessToken: String,
    val refreshToken: String,
)

data class RefreshResult(
    val userId: UserId,
    val tokens: IssuedTokens,
)

internal data class StoredRefreshToken(
    val userId: UserId,
    val expiresAt: OffsetDateTime,
    val revokedAt: OffsetDateTime?,
)
