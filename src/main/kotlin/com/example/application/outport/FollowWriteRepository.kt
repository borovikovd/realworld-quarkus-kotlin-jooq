package com.example.application.outport

import com.example.domain.aggregate.user.UserId

interface FollowWriteRepository {
    fun follow(
        followerId: UserId,
        followeeId: UserId,
    )

    fun unfollow(
        followerId: UserId,
        followeeId: UserId,
    )
}
