package com.example.application.port.outbound

import com.example.application.port.outbound.CommentReadModel

interface CommentReadRepository {
    fun getCommentById(
        id: Long,
        viewerId: Long?,
    ): CommentReadModel?

    fun getCommentsBySlug(
        slug: String,
        viewerId: Long?,
    ): List<CommentReadModel>
}
