package com.example.infrastructure.persistence.jooq.auth

import com.example.application.outport.RevokedTokenRepository
import com.example.jooq.auth.tables.references.REVOKED_TOKEN
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import java.time.OffsetDateTime
import java.util.UUID

@ApplicationScoped
class JooqRevokedTokenRepository(
    private val dsl: DSLContext,
) : RevokedTokenRepository {
    override fun insert(
        jti: UUID,
        userId: Long,
        expiresAt: OffsetDateTime,
    ) {
        dsl
            .insertInto(REVOKED_TOKEN)
            .set(REVOKED_TOKEN.JTI, jti)
            .set(REVOKED_TOKEN.USER_ID, userId)
            .set(REVOKED_TOKEN.EXPIRES_AT, expiresAt)
            .onConflict(REVOKED_TOKEN.JTI)
            .doNothing()
            .execute()
    }

    override fun isRevoked(jti: UUID): Boolean =
        dsl.fetchExists(
            dsl
                .selectOne()
                .from(REVOKED_TOKEN)
                .where(REVOKED_TOKEN.JTI.eq(jti))
                .and(REVOKED_TOKEN.EXPIRES_AT.gt(OffsetDateTime.now())),
        )

    override fun deleteExpiredBefore(before: OffsetDateTime): Int =
        dsl
            .deleteFrom(REVOKED_TOKEN)
            .where(REVOKED_TOKEN.EXPIRES_AT.lt(before))
            .execute()
}
