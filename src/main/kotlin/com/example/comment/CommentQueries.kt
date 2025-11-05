package com.example.comment

import com.example.shared.domain.Queries
import com.example.api.model.Comment as ApiComment

interface CommentQueries : Queries {
    fun getCommentsBySlug(
        slug: String,
        viewerId: Long? = null,
    ): List<ApiComment>

    fun getCommentById(
        commentId: Long,
        viewerId: Long? = null,
    ): ApiComment
}
