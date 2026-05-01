package com.example.application.outport.auth

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

    /** Returns true if the token was revoked, false if it was already revoked or not found. */
    fun revokeByHash(
        tokenHash: String,
        revokedAt: OffsetDateTime,
    ): Boolean

    fun revokeAllForUser(
        userId: UserId,
        revokedAt: OffsetDateTime,
    )

    /** Deletes tokens whose expiry is before [before]. Returns the number of rows deleted. */
    fun deleteExpiredBefore(before: OffsetDateTime): Int
}
