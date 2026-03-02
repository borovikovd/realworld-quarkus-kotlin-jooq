package com.example.profile

data class ProfileSummary(
    val username: String,
    val bio: String?,
    val image: String?,
    val following: Boolean,
)
