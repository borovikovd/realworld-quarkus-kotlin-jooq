package com.example.application.comment

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
