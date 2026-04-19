package com.example.application.profile

interface ProfileReadService {
    fun getProfileByUsername(
        username: String,
        viewerId: Long?,
    ): ProfileSummary
}
