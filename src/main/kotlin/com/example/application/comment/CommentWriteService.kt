package com.example.application.comment

interface CommentWriteService {
    fun addComment(
        articleSlug: String,
        body: String,
    ): Long

    fun deleteComment(
        articleSlug: String,
        commentId: Long,
    )
}
