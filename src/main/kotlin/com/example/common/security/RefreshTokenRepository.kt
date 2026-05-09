package com.example.common.security

import com.example.jooq.auth.tables.references.REFRESH_TOKEN
import com.example.user.UserId
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import java.time.OffsetDateTime

@ApplicationScoped
class RefreshTokenRepository(
    private val dsl: DSLContext,
) {
    fun store(
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

    fun findByHash(tokenHash: String): StoredRefreshToken? =
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

    /** Returns true if the token was revoked, false if it was already revoked or not found. */
    fun revokeByHash(
        tokenHash: String,
        revokedAt: OffsetDateTime,
    ): Boolean =
        dsl
            .update(REFRESH_TOKEN)
            .set(REFRESH_TOKEN.REVOKED_AT, revokedAt)
            .where(REFRESH_TOKEN.TOKEN_HASH.eq(tokenHash))
            .and(REFRESH_TOKEN.REVOKED_AT.isNull)
            .execute() > 0

    fun revokeAllForUser(
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

    /** Deletes tokens whose expiry is before [before]. Returns the number of rows deleted. */
    fun deleteExpiredBefore(before: OffsetDateTime): Int =
        dsl.deleteFrom(REFRESH_TOKEN).where(REFRESH_TOKEN.EXPIRES_AT.lt(before)).execute()
}
