package com.example.application.usecase.profile

import com.example.application.readmodel.ProfileReadModel

interface ProfileQueries {
    fun getProfileByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileReadModel?
}
