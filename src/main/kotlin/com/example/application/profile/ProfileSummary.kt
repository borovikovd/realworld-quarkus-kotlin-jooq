package com.example.application.profile

data class ProfileSummary(
    val username: String,
    val bio: String?,
    val image: String?,
    val following: Boolean,
)
