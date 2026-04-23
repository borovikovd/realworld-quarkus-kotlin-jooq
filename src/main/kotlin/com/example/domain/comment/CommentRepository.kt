package com.example.domain.comment

import com.example.domain.Repository
import com.example.domain.aggregate.article.ArticleId
import com.example.domain.aggregate.comment.Comment
import com.example.domain.aggregate.comment.CommentId

interface CommentRepository : Repository<Comment, CommentId> {
    fun findByArticleId(articleId: ArticleId): List<Comment>

    fun deleteById(id: CommentId)
}
