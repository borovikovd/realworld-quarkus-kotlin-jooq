package com.example.application.port.inbound.command

data class UpdateArticleCommand(
    val slug: String,
    val title: String?,
    val description: String?,
    val body: String?,
)
