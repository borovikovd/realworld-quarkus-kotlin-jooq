package com.example.application.user

data class UserSummary(
    val email: String,
    val token: String,
    val username: String,
    val bio: String?,
    val image: String?,
)
