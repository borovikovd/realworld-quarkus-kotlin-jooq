package com.example.application.port.inbound.command

data class CreateArticleCommand(
    val title: String,
    val description: String,
    val body: String,
    val tags: List<String>,
)
