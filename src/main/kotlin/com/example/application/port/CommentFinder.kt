package com.example.application.port

import com.example.application.readmodel.CommentReadModel
import com.example.domain.aggregate.comment.CommentId
import com.example.domain.aggregate.user.UserId

interface CommentFinder {
    fun findById(
        id: CommentId,
        viewerId: UserId?,
    ): CommentReadModel?

    fun findByArticleSlug(
        slug: String,
        viewerId: UserId?,
    ): List<CommentReadModel>
}
