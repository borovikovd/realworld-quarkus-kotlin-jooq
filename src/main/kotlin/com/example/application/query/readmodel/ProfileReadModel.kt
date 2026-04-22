package com.example.application.query.readmodel

data class ProfileReadModel(
    val username: String,
    val bio: String?,
    val image: String?,
    val following: Boolean,
)
