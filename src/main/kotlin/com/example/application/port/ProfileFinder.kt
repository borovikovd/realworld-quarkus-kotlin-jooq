package com.example.application.port

import com.example.application.readmodel.ProfileReadModel
import com.example.domain.aggregate.user.UserId

interface ProfileFinder {
    fun findByUsername(
        username: String,
        viewerId: UserId?,
    ): ProfileReadModel?
}
