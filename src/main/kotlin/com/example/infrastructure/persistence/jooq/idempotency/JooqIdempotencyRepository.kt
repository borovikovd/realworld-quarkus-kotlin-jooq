package com.example.infrastructure.persistence.jooq.idempotency

import com.example.application.port.IdempotencyRepository
import com.example.application.readmodel.StoredIdempotencyKey
import com.example.jooq.public.tables.references.IDEMPOTENCY_KEY
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import java.time.OffsetDateTime

@ApplicationScoped
class JooqIdempotencyRepository(
    private val dsl: DSLContext,
) : IdempotencyRepository {
    override fun insertProcessing(
        key: String,
        scope: String,
        requestPath: String,
        expiresAt: OffsetDateTime,
    ): Boolean =
        dsl
            .insertInto(IDEMPOTENCY_KEY)
            .set(IDEMPOTENCY_KEY.KEY, key)
            .set(IDEMPOTENCY_KEY.SCOPE, scope)
            .set(IDEMPOTENCY_KEY.REQUEST_PATH, requestPath)
            .set(IDEMPOTENCY_KEY.EXPIRES_AT, expiresAt)
            .onConflict(IDEMPOTENCY_KEY.KEY, IDEMPOTENCY_KEY.SCOPE)
            .doNothing()
            .execute() > 0

    override fun findByKeyAndScope(
        key: String,
        scope: String,
    ): StoredIdempotencyKey? =
        dsl
            .select(IDEMPOTENCY_KEY.REQUEST_PATH, IDEMPOTENCY_KEY.RESPONSE_STATUS, IDEMPOTENCY_KEY.RESPONSE_BODY)
            .from(IDEMPOTENCY_KEY)
            .where(IDEMPOTENCY_KEY.KEY.eq(key))
            .and(IDEMPOTENCY_KEY.SCOPE.eq(scope))
            .and(IDEMPOTENCY_KEY.EXPIRES_AT.gt(OffsetDateTime.now()))
            .fetchOne()
            ?.let {
                StoredIdempotencyKey(
                    requestPath = it.get(IDEMPOTENCY_KEY.REQUEST_PATH)!!,
                    responseStatus = it.get(IDEMPOTENCY_KEY.RESPONSE_STATUS),
                    responseBody = it.get(IDEMPOTENCY_KEY.RESPONSE_BODY),
                )
            }

    override fun complete(
        key: String,
        scope: String,
        responseStatus: Int,
        responseBody: String,
    ) {
        dsl
            .update(IDEMPOTENCY_KEY)
            .set(IDEMPOTENCY_KEY.RESPONSE_STATUS, responseStatus)
            .set(IDEMPOTENCY_KEY.RESPONSE_BODY, responseBody)
            .where(IDEMPOTENCY_KEY.KEY.eq(key))
            .and(IDEMPOTENCY_KEY.SCOPE.eq(scope))
            .and(IDEMPOTENCY_KEY.RESPONSE_BODY.isNull)
            .execute()
    }

    override fun deleteByKeyAndScope(
        key: String,
        scope: String,
    ) {
        dsl
            .deleteFrom(IDEMPOTENCY_KEY)
            .where(IDEMPOTENCY_KEY.KEY.eq(key))
            .and(IDEMPOTENCY_KEY.SCOPE.eq(scope))
            .execute()
    }

    override fun deleteExpiredBefore(before: OffsetDateTime): Int =
        dsl
            .deleteFrom(IDEMPOTENCY_KEY)
            .where(IDEMPOTENCY_KEY.EXPIRES_AT.lt(before))
            .execute()
}
