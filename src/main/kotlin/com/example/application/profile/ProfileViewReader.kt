package com.example.application.profile

import com.example.application.profile.readmodel.ProfileView

interface ProfileViewReader {
    fun getProfileByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileView
}
