package com.example.profile.application

interface ProfileReadService {
    fun getProfileByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileSummary
}
