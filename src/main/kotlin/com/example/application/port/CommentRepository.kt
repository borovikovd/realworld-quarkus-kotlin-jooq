package com.example.application.port

import com.example.domain.aggregate.comment.Comment
import com.example.domain.aggregate.comment.CommentId

interface CommentRepository : Repository<Comment, CommentId> {
    fun deleteById(id: CommentId)
}
