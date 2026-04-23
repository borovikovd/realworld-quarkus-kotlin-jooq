package com.example.application.port.inbound.command

data class DeleteCommentCommand(
    val articleSlug: String,
    val commentId: Long,
)
