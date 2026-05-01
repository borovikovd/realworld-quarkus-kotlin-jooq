package com.example.application.outport.comment

import com.example.application.outport.Repository
import com.example.application.readmodel.CommentReadModel
import com.example.domain.aggregate.article.ArticleId
import com.example.domain.aggregate.comment.Comment
import com.example.domain.aggregate.comment.CommentId

interface CommentRepository : Repository<Comment, CommentId> {
    fun findByArticleId(articleId: ArticleId): List<Comment>

    fun deleteById(id: CommentId)

    fun findById(
        id: Long,
        viewerId: Long?,
    ): CommentReadModel?

    fun findByArticleSlug(
        slug: String,
        viewerId: Long?,
    ): List<CommentReadModel>
}
