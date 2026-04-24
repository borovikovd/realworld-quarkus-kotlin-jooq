package com.example.application.inport.query

import com.example.application.readmodel.CommentReadModel

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
