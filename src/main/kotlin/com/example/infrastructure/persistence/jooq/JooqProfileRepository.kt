package com.example.infrastructure.persistence.jooq

import com.example.application.port.ProfileRepository
import com.example.domain.aggregate.user.UserId
import com.example.jooq.public.tables.references.FOLLOWERS
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext

@ApplicationScoped
class JooqProfileRepository(
    private val dsl: DSLContext,
) : ProfileRepository {
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
