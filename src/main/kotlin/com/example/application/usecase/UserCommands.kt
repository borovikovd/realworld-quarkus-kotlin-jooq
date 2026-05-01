package com.example.application.usecase

import com.example.application.readmodel.AuthenticatedUser
import java.util.UUID

interface UserCommands {
    fun register(
        email: String,
        username: String,
        password: String,
    ): AuthenticatedUser

    fun login(
        email: String,
        password: String,
    ): AuthenticatedUser

    fun updateUser(
        userId: Long,
        email: String?,
        username: String?,
        password: String?,
        bio: String?,
        image: String?,
    ): AuthenticatedUser

    fun eraseUser(userId: Long)

    fun refresh(refreshToken: String): AuthenticatedUser

    fun logout(
        refreshToken: String,
        jti: UUID?,
        userId: Long?,
    )
}
