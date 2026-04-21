package com.example.application.comment

import com.example.application.comment.readmodel.CommentView

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
