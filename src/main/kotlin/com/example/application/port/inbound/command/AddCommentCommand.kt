package com.example.application.port.inbound.command

data class AddCommentCommand(
    val articleSlug: String,
    val body: String,
)
