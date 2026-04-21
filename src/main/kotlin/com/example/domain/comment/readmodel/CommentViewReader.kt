package com.example.domain.comment.readmodel

interface CommentViewReader {
    fun getCommentById(
        id: Long,
        viewerId: Long?,
    ): CommentView

    fun getCommentsBySlug(
        slug: String,
        viewerId: Long?,
    ): List<CommentView>
}
