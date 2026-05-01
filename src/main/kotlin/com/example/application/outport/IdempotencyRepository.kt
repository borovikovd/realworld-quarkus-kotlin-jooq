package com.example.application.outport

import com.example.application.readmodel.StoredIdempotencyKey
import java.time.OffsetDateTime

interface IdempotencyRepository {
    /** Returns true if the record was inserted, false if an identical (key, scope) already exists. */
    fun insertProcessing(
        key: String,
        scope: String,
        requestPath: String,
        expiresAt: OffsetDateTime,
    ): Boolean

    fun findByKeyAndScope(
        key: String,
        scope: String,
    ): StoredIdempotencyKey?

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
