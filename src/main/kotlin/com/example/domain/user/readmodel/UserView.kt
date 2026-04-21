package com.example.domain.user.readmodel

import com.example.domain.user.Email
import com.example.domain.user.UserId
import com.example.domain.user.Username

data class UserView(
    val id: UserId,
    val email: Email,
    val username: Username,
    val bio: String?,
    val image: String?,
)
