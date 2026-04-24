package com.example.application.outport

import com.example.domain.aggregate.article.ArticleId
import com.example.domain.aggregate.comment.Comment
import com.example.domain.aggregate.comment.CommentId

interface CommentWriteRepository : Repository<Comment, CommentId> {
    fun findByArticleId(articleId: ArticleId): List<Comment>

    fun deleteById(id: CommentId)
}
