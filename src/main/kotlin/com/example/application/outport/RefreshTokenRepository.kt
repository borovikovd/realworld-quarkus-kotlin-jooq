package com.example.application.outport

import com.example.application.readmodel.StoredRefreshToken
import com.example.domain.aggregate.user.UserId
import java.time.OffsetDateTime

interface RefreshTokenRepository {
    fun store(
        userId: UserId,
        tokenHash: String,
        expiresAt: OffsetDateTime,
    )

    fun findByHash(tokenHash: String): StoredRefreshToken?

    fun revokeByHash(
        tokenHash: String,
        revokedAt: OffsetDateTime,
    )

    fun revokeAllForUser(
        userId: UserId,
        revokedAt: OffsetDateTime,
    )
}
