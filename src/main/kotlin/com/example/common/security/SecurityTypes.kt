package com.example.common.security

import com.example.user.UserId
import java.time.OffsetDateTime
import java.util.UUID

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
    val userId: UserId,
    val accessToken: String,
    val refreshToken: String,
)

internal data class StoredRefreshToken(
    val userId: UserId,
    val familyId: UUID,
    val expiresAt: OffsetDateTime,
    val revokedAt: OffsetDateTime?,
)
