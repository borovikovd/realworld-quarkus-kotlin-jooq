package com.example.application.usecase

import com.example.application.readmodel.ProfileReadModel
import com.example.domain.aggregate.user.UserId

interface ProfileQueries {
    fun getProfileByUsername(
        username: String,
        viewerId: UserId?,
    ): ProfileReadModel?
}
