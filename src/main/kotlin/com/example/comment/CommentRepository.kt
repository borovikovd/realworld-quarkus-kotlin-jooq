package com.example.comment

import com.example.article.ArticleId
import com.example.shared.domain.Repository

interface CommentRepository : Repository<Comment, CommentId> {
    fun findByArticleId(articleId: ArticleId): List<Comment>

    fun deleteById(id: CommentId)
}
