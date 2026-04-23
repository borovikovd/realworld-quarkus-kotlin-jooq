package com.example.application.port.inbound.command

data class LoginUserCommand(
    val email: String,
    val password: String,
)
