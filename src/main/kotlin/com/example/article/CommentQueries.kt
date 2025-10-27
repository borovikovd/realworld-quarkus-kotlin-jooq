package com.example.article

import com.example.api.model.Comment as ApiComment

/**
 * Query interface for Comment read operations (CQRS Read Model)
 *
 * Purpose:
 * - Abstracts query implementation details from consumers
 * - Enables testing with mock implementations
 * - Follows Dependency Inversion Principle
 *
 * Design Decision:
 * - Returns API DTOs (ApiComment) optimized for presentation
 * - Separate from command side (CommentService returns domain entities)
 */
interface CommentQueries {
    /**
     * Get all comments for an article by slug
     *
     * @param slug Article slug
     * @param viewerId Optional current user ID for following status
     * @return List of comment DTOs with author profiles, ordered by creation date (oldest first)
     */
    fun getCommentsBySlug(
        slug: String,
        viewerId: Long? = null,
    ): List<ApiComment>

    /**
     * Get single comment by ID
     *
     * @param commentId Comment ID
     * @param viewerId Optional current user ID for following status
     * @return Comment DTO with author profile
     */
    fun getCommentById(
        commentId: Long,
        viewerId: Long? = null,
    ): ApiComment
}
