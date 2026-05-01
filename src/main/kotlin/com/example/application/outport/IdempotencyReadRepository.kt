package com.example.application.outport

import com.example.application.readmodel.StoredIdempotencyKey

interface IdempotencyReadRepository {
    fun findByKeyAndScope(
        key: String,
        scope: String,
    ): StoredIdempotencyKey?
}
