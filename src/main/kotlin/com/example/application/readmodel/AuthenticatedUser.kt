package com.example.application.readmodel

data class AuthenticatedUser(
    val user: UserReadModel,
    val accessToken: String,
    val refreshToken: String,
)
