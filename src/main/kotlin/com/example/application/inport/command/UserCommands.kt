package com.example.application.inport.command

interface UserCommands {
    fun register(
        email: String,
        username: String,
        password: String,
    ): Long

    fun login(
        email: String,
        password: String,
    ): Long

    fun updateUser(
        userId: Long,
        email: String?,
        username: String?,
        password: String?,
        bio: String?,
        image: String?,
    ): Long

    fun eraseUser(userId: Long)

    fun refresh(refreshToken: String): Long

    fun logout(refreshToken: String)
}
