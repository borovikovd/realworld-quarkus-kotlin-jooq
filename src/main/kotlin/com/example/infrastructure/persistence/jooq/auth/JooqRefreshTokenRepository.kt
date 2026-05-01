package com.example.infrastructure.persistence.jooq.auth

import com.example.application.outport.security.RefreshTokenRepository
import com.example.application.readmodel.StoredRefreshToken
import com.example.domain.aggregate.user.UserId
import com.example.jooq.auth.tables.references.REFRESH_TOKEN
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import java.time.OffsetDateTime

@ApplicationScoped
class JooqRefreshTokenRepository(
    private val dsl: DSLContext,
) : RefreshTokenRepository {
    override fun store(
        userId: UserId,
        tokenHash: String,
        expiresAt: OffsetDateTime,
    ) {
        dsl
            .insertInto(REFRESH_TOKEN)
            .set(REFRESH_TOKEN.USER_ID, userId.value)
            .set(REFRESH_TOKEN.TOKEN_HASH, tokenHash)
            .set(REFRESH_TOKEN.EXPIRES_AT, expiresAt)
            .execute()
    }

    override fun findByHash(tokenHash: String): StoredRefreshToken? =
        dsl
            .select(REFRESH_TOKEN.USER_ID, REFRESH_TOKEN.EXPIRES_AT, REFRESH_TOKEN.REVOKED_AT)
            .from(REFRESH_TOKEN)
            .where(REFRESH_TOKEN.TOKEN_HASH.eq(tokenHash))
            .fetchOne()
            ?.let {
                StoredRefreshToken(
                    userId = UserId(it.get(REFRESH_TOKEN.USER_ID)!!),
                    expiresAt = it.get(REFRESH_TOKEN.EXPIRES_AT)!!,
                    revokedAt = it.get(REFRESH_TOKEN.REVOKED_AT),
                )
            }

    override fun revokeByHash(
        tokenHash: String,
        revokedAt: OffsetDateTime,
    ): Boolean =
        dsl
            .update(REFRESH_TOKEN)
            .set(REFRESH_TOKEN.REVOKED_AT, revokedAt)
            .where(REFRESH_TOKEN.TOKEN_HASH.eq(tokenHash))
            .and(REFRESH_TOKEN.REVOKED_AT.isNull)
            .execute() > 0

    override fun revokeAllForUser(
        userId: UserId,
        revokedAt: OffsetDateTime,
    ) {
        dsl
            .update(REFRESH_TOKEN)
            .set(REFRESH_TOKEN.REVOKED_AT, revokedAt)
            .where(REFRESH_TOKEN.USER_ID.eq(userId.value))
            .and(REFRESH_TOKEN.REVOKED_AT.isNull)
            .execute()
    }

    override fun deleteExpiredBefore(before: OffsetDateTime): Int =
        dsl
            .deleteFrom(REFRESH_TOKEN)
            .where(REFRESH_TOKEN.EXPIRES_AT.lt(before))
            .execute()
}
