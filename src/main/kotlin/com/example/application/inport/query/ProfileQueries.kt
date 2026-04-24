package com.example.application.inport.query

import com.example.application.inport.query.readmodel.ProfileReadModel

interface ProfileQueries {
    fun getProfileByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileReadModel?
}
