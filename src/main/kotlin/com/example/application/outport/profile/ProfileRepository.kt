package com.example.application.outport.profile

import com.example.application.readmodel.ProfileReadModel
import com.example.domain.aggregate.user.UserId

interface ProfileRepository {
    fun findByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileReadModel?

    fun follow(
        followerId: UserId,
        followeeId: UserId,
    )

    fun unfollow(
        followerId: UserId,
        followeeId: UserId,
    )
}
