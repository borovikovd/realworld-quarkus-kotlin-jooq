package com.example.comment.application

interface CommentReadService {
    fun hydrate(
        id: Long,
        viewerId: Long?,
    ): CommentSummary

    fun getCommentsBySlug(
        slug: String,
        viewerId: Long?,
    ): List<CommentSummary>
}
