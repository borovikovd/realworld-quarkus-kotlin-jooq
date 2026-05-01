package com.example.application.usecase.comment

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
