package com.example.profile

import com.example.api.model.Profile

interface ProfileQueries {
    fun getProfileByUsername(
        username: String,
        viewerId: Long? = null,
    ): Profile
}
