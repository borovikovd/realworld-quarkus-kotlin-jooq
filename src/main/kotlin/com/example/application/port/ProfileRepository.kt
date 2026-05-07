package com.example.application.port

import com.example.domain.aggregate.user.UserId

interface ProfileRepository {
    fun follow(
        followerId: UserId,
        followeeId: UserId,
    )

    fun unfollow(
        followerId: UserId,
        followeeId: UserId,
    )
}
