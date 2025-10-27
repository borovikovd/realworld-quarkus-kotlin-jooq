package com.example.article

import com.example.shared.domain.Entity
import java.time.OffsetDateTime

/**
 * Article Aggregate Root
 *
 * This is the root entity of the Article aggregate, which includes:
 * - Article data (title, description, body, slug)
 * - Tags (value objects managed as part of article lifecycle)
 * - Favorites (relationships managed by ArticleRepository)
 *
 * Aggregate Boundary:
 * - Tags are part of this aggregate and persisted/loaded together with Article
 * - Favorites are technically part of this aggregate but managed separately for performance
 * - Comments are a SEPARATE aggregate (see Comment.kt)
 *
 * Transactional Consistency:
 * - Changes to Article + Tags happen atomically within single transaction
 * - Repository ensures aggregate integrity during persistence
 *
 * Business Rules:
 * - Only author can update/delete article
 * - Slug is unique and generated from title
 * - Title, description, and body must not be blank
 */
data class Article(
    override val id: Long? = null,
    val slug: String,
    val title: String,
    val description: String,
    val body: String,
    val authorId: Long,
    val tags: Set<String> = emptySet(),
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
) : Entity<Long> {
    init {
        require(title.isNotBlank()) { "Title must not be blank" }
        require(description.isNotBlank()) { "Description must not be blank" }
        require(body.isNotBlank()) { "Body must not be blank" }
    }

    override fun withId(newId: Long): Article = copy(id = newId)

    fun update(
        slug: String,
        title: String,
        description: String,
        body: String,
    ): Article =
        copy(
            slug = slug,
            title = title,
            description = description,
            body = body,
            updatedAt = OffsetDateTime.now(),
        )

    fun canBeDeletedBy(userId: Long): Boolean = userId == authorId
}
