package com.example.application.port.inbound.command

data class UpdateUserCommand(
    val userId: Long,
    val email: String?,
    val username: String?,
    val password: String?,
    val bio: String?,
    val image: String?,
)
