package com.example.application.readmodel

import com.example.domain.aggregate.user.PasswordHash
import com.example.domain.aggregate.user.UserId

data class LoginCredentials(
    val userId: UserId,
    val passwordHash: PasswordHash,
)
