package com.example.domain.profile

import com.example.domain.aggregate.user.UserId

interface FollowRepository {
    fun follow(
        followerId: UserId,
        followeeId: UserId,
    )

    fun unfollow(
        followerId: UserId,
        followeeId: UserId,
    )

    fun isFollowing(
        followerId: UserId,
        followeeId: UserId,
    ): Boolean
}
