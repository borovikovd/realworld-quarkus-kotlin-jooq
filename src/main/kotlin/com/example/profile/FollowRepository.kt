package com.example.profile

import com.example.user.UserId

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
