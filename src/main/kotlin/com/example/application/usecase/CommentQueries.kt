package com.example.application.usecase

import com.example.application.readmodel.CommentReadModel
import com.example.domain.aggregate.comment.CommentId
import com.example.domain.aggregate.user.UserId

interface CommentQueries {
    fun getCommentById(
        id: CommentId,
        viewerId: UserId?,
    ): CommentReadModel?

    fun getCommentsBySlug(
        slug: String,
        viewerId: UserId?,
    ): List<CommentReadModel>
}
