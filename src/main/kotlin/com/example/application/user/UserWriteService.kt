package com.example.application.user

interface UserWriteService {
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
}
