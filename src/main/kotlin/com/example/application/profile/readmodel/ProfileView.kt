package com.example.application.profile.readmodel

data class ProfileView(
    val username: String,
    val bio: String?,
    val image: String?,
    val following: Boolean,
)
