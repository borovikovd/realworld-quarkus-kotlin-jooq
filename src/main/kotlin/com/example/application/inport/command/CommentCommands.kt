package com.example.application.inport.command

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
