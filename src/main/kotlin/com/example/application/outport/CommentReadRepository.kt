package com.example.application.outport

import com.example.application.readmodel.CommentReadModel

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
