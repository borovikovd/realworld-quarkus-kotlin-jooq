package com.example.application.query.readmodel

import com.example.domain.user.Email
import com.example.domain.user.UserId
import com.example.domain.user.Username

data class UserReadModel(
    val id: UserId,
    val email: Email,
    val username: Username,
    val bio: String?,
    val image: String?,
)
