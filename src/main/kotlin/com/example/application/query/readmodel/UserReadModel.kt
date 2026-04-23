package com.example.application.query.readmodel

import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.UserId
import com.example.domain.aggregate.user.Username

data class UserReadModel(
    val id: UserId,
    val email: Email,
    val username: Username,
    val bio: String?,
    val image: String?,
)
