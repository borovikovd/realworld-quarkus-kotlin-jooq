package com.example.application.usecase

interface CommentCommands {
    fun addComment(
        articleSlug: String,
        body: String,
    ): Long

    fun deleteComment(
        articleSlug: String,
        commentId: Long,
    )
}
