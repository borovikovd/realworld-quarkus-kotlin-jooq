package com.example.comment

import com.example.api.model.Comment as ApiComment
import com.example.shared.domain.Queries

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
