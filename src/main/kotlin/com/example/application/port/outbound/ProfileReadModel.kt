package com.example.application.port.outbound

data class ProfileReadModel(
    val username: String,
    val bio: String?,
    val image: String?,
    val following: Boolean,
)
