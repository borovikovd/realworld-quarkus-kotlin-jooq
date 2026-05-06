package com.example.infrastructure.persistence.jooq

import com.example.application.port.ProfileRepository
import com.example.application.port.security.CryptoService
import com.example.application.readmodel.ProfileReadModel
import com.example.domain.aggregate.user.UserId
import com.example.infrastructure.persistence.jooq.shared.decryptAuthorProfile
import com.example.jooq.public.tables.references.FOLLOWERS
import com.example.jooq.public.tables.references.USER
import com.example.jooq.vault.tables.references.PERSON
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.select

@ApplicationScoped
class JooqProfileRepository(
    private val dsl: DSLContext,
    private val crypto: CryptoService,
) : ProfileRepository {
    override fun findByUsername(
        username: String,
        viewerId: UserId?,
    ): ProfileReadModel? {
        val usernameHash = crypto.hmacUsername(username)
        return dsl
            .select(
                USER.ID,
                PERSON.USERNAME_ENC,
                PERSON.BIO_ENC,
                PERSON.IMAGE_ENC,
                viewerId?.let {
                    select(count())
                        .from(FOLLOWERS)
                        .where(FOLLOWERS.FOLLOWEE_ID.eq(PERSON.USER_ID))
                        .and(FOLLOWERS.FOLLOWER_ID.eq(it.value))
                        .asField("following")
                } ?: org.jooq.impl.DSL
                    .`val`(0)
                    .`as`("following"),
            ).from(USER)
            .join(PERSON)
            .on(PERSON.USER_ID.eq(USER.ID))
            .where(PERSON.USERNAME_HASH.eq(usernameHash))
            .and(USER.DELETED_AT.isNull)
            .fetchOne()
            ?.let { record ->
                record.decryptAuthorProfile(
                    crypto = crypto,
                    userId = record.get(USER.ID),
                    following = record.get("following", Int::class.java) > 0,
                )
            }
    }

    override fun follow(
        followerId: UserId,
        followeeId: UserId,
    ) {
        dsl
            .insertInto(FOLLOWERS)
            .set(FOLLOWERS.FOLLOWER_ID, followerId.value)
            .set(FOLLOWERS.FOLLOWEE_ID, followeeId.value)
            .onDuplicateKeyIgnore()
            .execute()
    }

    override fun unfollow(
        followerId: UserId,
        followeeId: UserId,
    ) {
        dsl
            .deleteFrom(FOLLOWERS)
            .where(FOLLOWERS.FOLLOWER_ID.eq(followerId.value))
            .and(FOLLOWERS.FOLLOWEE_ID.eq(followeeId.value))
            .execute()
    }
}
