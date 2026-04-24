package com.example.application.readmodel

data class ProfileReadModel(
    val username: String,
    val bio: String?,
    val image: String?,
    val following: Boolean,
)
