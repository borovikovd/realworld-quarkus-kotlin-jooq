package com.example.application.comment

interface CommentViewReader {
    fun hydrate(
        id: Long,
        viewerId: Long?,
    ): CommentView

    fun getCommentsBySlug(
        slug: String,
        viewerId: Long?,
    ): List<CommentView>
}
