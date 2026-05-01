package com.example.application.outport.security

import java.time.OffsetDateTime
import java.util.UUID

interface RevokedTokenRepository {
    fun insert(
        jti: UUID,
        userId: Long,
        expiresAt: OffsetDateTime,
    )

    fun isRevoked(jti: UUID): Boolean

    fun deleteExpiredBefore(before: OffsetDateTime): Int
}
