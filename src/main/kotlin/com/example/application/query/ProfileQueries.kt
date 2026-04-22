package com.example.application.query

import com.example.application.query.readmodel.ProfileReadModel

interface ProfileQueries {
    fun getProfileByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileReadModel?
}
