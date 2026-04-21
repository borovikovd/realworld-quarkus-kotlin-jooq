package com.example.domain.profile.readmodel

interface ProfileViewReader {
    fun getProfileByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileView
}
