package com.example.domain.comment

import com.example.domain.article.ArticleId
import com.example.domain.shared.Repository

interface CommentRepository : Repository<Comment, CommentId> {
    fun findByArticleId(articleId: ArticleId): List<Comment>

    fun deleteById(id: CommentId)
}
