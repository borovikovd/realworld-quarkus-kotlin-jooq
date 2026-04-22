package com.example.application.query

import com.example.application.query.readmodel.CommentReadModel

interface CommentQueries {
    fun getCommentById(
        id: Long,
        viewerId: Long?,
    ): CommentReadModel?

    fun getCommentsBySlug(
        slug: String,
        viewerId: Long?,
    ): List<CommentReadModel>
}
