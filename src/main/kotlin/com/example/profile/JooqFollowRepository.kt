package com.example.profile

import com.example.jooq.public.tables.references.FOLLOWERS
import jakarta.enterprise.context.ApplicationScoped
import org.jooq.DSLContext

@ApplicationScoped
class JooqFollowRepository(
    private val dsl: DSLContext,
) : FollowRepository {
    override fun follow(
        followerId: Long,
        followeeId: Long,
    ) {
        dsl
            .insertInto(FOLLOWERS)
            .set(FOLLOWERS.FOLLOWER_ID, followerId)
            .set(FOLLOWERS.FOLLOWEE_ID, followeeId)
            .onDuplicateKeyIgnore()
            .execute()
    }

    override fun unfollow(
        followerId: Long,
        followeeId: Long,
    ) {
        dsl
            .deleteFrom(FOLLOWERS)
            .where(FOLLOWERS.FOLLOWER_ID.eq(followerId))
            .and(FOLLOWERS.FOLLOWEE_ID.eq(followeeId))
            .execute()
    }

    override fun isFollowing(
        followerId: Long,
        followeeId: Long,
    ): Boolean =
        dsl.fetchExists(
            dsl
                .selectFrom(FOLLOWERS)
                .where(FOLLOWERS.FOLLOWER_ID.eq(followerId))
                .and(FOLLOWERS.FOLLOWEE_ID.eq(followeeId)),
        )
}
