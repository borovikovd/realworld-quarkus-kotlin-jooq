package com.example.application.port.outbound

import com.example.application.query.readmodel.CommentReadModel

interface CommentReadRepository {
    fun findById(
        id: Long,
        viewerId: Long?,
    ): CommentReadModel?

    fun findByArticleSlug(
        slug: String,
        viewerId: Long?,
    ): List<CommentReadModel>
}
