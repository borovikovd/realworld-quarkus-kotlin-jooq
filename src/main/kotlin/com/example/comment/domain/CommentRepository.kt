package com.example.comment.domain

import com.example.article.domain.ArticleId
import com.example.shared.domain.Repository

interface CommentRepository : Repository<Comment, CommentId> {
    fun findByArticleId(articleId: ArticleId): List<Comment>

    fun deleteById(id: CommentId)
}
