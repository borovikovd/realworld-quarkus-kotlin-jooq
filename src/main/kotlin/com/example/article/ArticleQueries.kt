package com.example.article

import com.example.api.model.Article as ApiArticle

/**
 * Query interface for Article read operations (CQRS Read Model)
 *
 * Purpose:
 * - Abstracts query implementation details from consumers
 * - Enables testing with mock implementations
 * - Follows Dependency Inversion Principle (depend on abstraction, not concrete jOOQ)
 *
 * Design Decision:
 * - Returns API DTOs (ApiArticle) optimized for presentation
 * - Separate from command side (ArticleService returns domain entities)
 * - Implementations can optimize queries without affecting domain model
 *
 * Implementation:
 * - JooqArticleQueries: Production implementation using jOOQ with optimized queries
 * - Test implementations: Can mock or use in-memory data
 */
interface ArticleQueries {
    /**
     * Get single article by slug with viewer-specific data
     *
     * @param slug Article slug (unique identifier)
     * @param viewerId Optional current user ID for favorited/following status
     * @return Article DTO with author profile, tags, favorites count
     */
    fun getArticleBySlug(
        slug: String,
        viewerId: Long? = null,
    ): ApiArticle

    /**
     * Get list of articles with optional filters
     *
     * @param tag Filter by tag name
     * @param author Filter by author username
     * @param favorited Filter by username who favorited
     * @param limit Maximum number of articles (default 20)
     * @param offset Pagination offset (default 0)
     * @param viewerId Optional current user ID for favorited/following status
     * @return List of article DTOs ordered by creation date (newest first)
     */
    fun getArticles(
        tag: String? = null,
        author: String? = null,
        favorited: String? = null,
        limit: Int = 20,
        offset: Int = 0,
        viewerId: Long? = null,
    ): List<ApiArticle>

    /**
     * Get articles feed for authenticated user (from followed authors)
     *
     * @param limit Maximum number of articles (default 20)
     * @param offset Pagination offset (default 0)
     * @param viewerId Current user ID (required)
     * @return List of article DTOs from followed authors, ordered by creation date
     */
    fun getArticlesFeed(
        limit: Int = 20,
        offset: Int = 0,
        viewerId: Long,
    ): List<ApiArticle>
}
