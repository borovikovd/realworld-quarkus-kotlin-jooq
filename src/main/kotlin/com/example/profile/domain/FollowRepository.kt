package com.example.profile.domain

import com.example.user.domain.UserId

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
