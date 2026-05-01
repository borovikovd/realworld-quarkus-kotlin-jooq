package com.example.application.outport

import com.example.application.readmodel.StoredIdempotencyKey
import java.time.OffsetDateTime

interface IdempotencyRepository {
    fun findByKeyAndScope(
        key: String,
        scope: String,
    ): StoredIdempotencyKey?

    fun insertProcessing(
        key: String,
        scope: String,
        requestPath: String,
        expiresAt: OffsetDateTime,
    ): Boolean

    fun complete(
        key: String,
        scope: String,
        responseStatus: Int,
        responseBody: String,
    )

    fun deleteByKeyAndScope(
        key: String,
        scope: String,
    )

    fun deleteExpiredBefore(before: OffsetDateTime): Int
}
