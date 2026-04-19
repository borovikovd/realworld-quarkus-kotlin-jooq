package com.example.application.profile

interface ProfileViewReader {
    fun getProfileByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileView
}
