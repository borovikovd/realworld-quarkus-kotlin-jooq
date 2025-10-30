package com.example.comment

import com.example.shared.domain.Repository

interface CommentRepository : Repository<Comment, Long> {
    fun findByArticleId(articleId: Long): List<Comment>

    fun deleteById(id: Long)
}
