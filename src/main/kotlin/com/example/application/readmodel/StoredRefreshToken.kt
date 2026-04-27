package com.example.application.readmodel

import com.example.domain.aggregate.user.UserId
import java.time.OffsetDateTime

data class StoredRefreshToken(
    val userId: UserId,
    val expiresAt: OffsetDateTime,
    val revokedAt: OffsetDateTime?,
)
