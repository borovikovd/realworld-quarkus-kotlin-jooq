package com.example.profile

import com.example.api.model.Profile
import com.example.shared.domain.Queries

interface ProfileQueries : Queries {
    fun getProfileByUsername(
        username: String,
        viewerId: Long? = null,
    ): Profile
}
