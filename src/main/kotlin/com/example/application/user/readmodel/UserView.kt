package com.example.application.user.readmodel

data class UserView(
    val email: String,
    val token: String,
    val username: String,
    val bio: String?,
    val image: String?,
)
