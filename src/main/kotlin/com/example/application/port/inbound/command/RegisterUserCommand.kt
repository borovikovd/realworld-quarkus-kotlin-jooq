package com.example.application.port.inbound.command

data class RegisterUserCommand(
    val email: String,
    val username: String,
    val password: String,
)
