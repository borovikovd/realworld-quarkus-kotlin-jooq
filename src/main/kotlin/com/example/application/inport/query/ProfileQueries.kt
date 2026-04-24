package com.example.application.inport.query

import com.example.application.readmodel.ProfileReadModel

interface ProfileQueries {
    fun getProfileByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileReadModel?
}
