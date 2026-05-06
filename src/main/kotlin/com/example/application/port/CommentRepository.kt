package com.example.application.port

import com.example.application.port.Repository
import com.example.application.readmodel.CommentReadModel
import com.example.domain.aggregate.article.ArticleId
import com.example.domain.aggregate.comment.Comment
import com.example.domain.aggregate.comment.CommentId
import com.example.domain.aggregate.user.UserId

interface CommentRepository : Repository<Comment, CommentId> {
    fun findByArticleId(articleId: ArticleId): List<Comment>

    fun deleteById(id: CommentId)

    fun findById(
        id: CommentId,
        viewerId: UserId?,
    ): CommentReadModel?

    fun findByArticleSlug(
        slug: String,
        viewerId: UserId?,
    ): List<CommentReadModel>
}
