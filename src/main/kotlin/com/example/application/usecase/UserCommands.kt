package com.example.application.usecase

import com.example.application.readmodel.AuthenticatedUser
import com.example.domain.aggregate.user.UserId
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
        userId: UserId,
        email: String?,
        username: String?,
        password: String?,
        bio: String?,
        image: String?,
    ): AuthenticatedUser

    fun eraseUser(
        userId: UserId,
        jti: UUID?,
    )

    fun refresh(refreshToken: String): AuthenticatedUser

    fun logout(
        refreshToken: String,
        jti: UUID?,
        userId: UserId?,
    )
}
