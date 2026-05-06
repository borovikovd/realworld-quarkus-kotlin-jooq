package com.example.application.usecase

import com.example.domain.aggregate.comment.CommentId

interface CommentCommands {
    fun addComment(
        articleSlug: String,
        body: String,
    ): CommentId

    fun deleteComment(
        articleSlug: String,
        commentId: CommentId,
    )
}
