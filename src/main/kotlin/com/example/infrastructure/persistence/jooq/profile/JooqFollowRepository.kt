package com.example.infrastructure.persistence.jooq.profile

import com.example.domain.aggregate.user.UserId
import com.example.domain.profile.FollowRepository
import com.example.jooq.public.tables.references.FOLLOWERS
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext

@ApplicationScoped
class JooqFollowRepository(
    private val dsl: DSLContext,
) : FollowRepository {
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

    override fun isFollowing(
        followerId: UserId,
        followeeId: UserId,
    ): Boolean =
        dsl.fetchExists(
            dsl
                .selectFrom(FOLLOWERS)
                .where(FOLLOWERS.FOLLOWER_ID.eq(followerId.value))
                .and(FOLLOWERS.FOLLOWEE_ID.eq(followeeId.value)),
        )
}
