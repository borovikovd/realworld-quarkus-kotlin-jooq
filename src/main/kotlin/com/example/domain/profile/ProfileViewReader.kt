package com.example.domain.profile

import com.example.domain.profile.readmodel.ProfileView

interface ProfileViewReader {
    fun getProfileByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileView
}
