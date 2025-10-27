package com.example.article

import com.example.shared.domain.Entity
import com.example.shared.exceptions.ForbiddenException
import java.time.OffsetDateTime

/**
 * Comment Aggregate Root (Separate from Article)
 *
 * Package Location Rationale:
 * - Located in article/ package for pragmatic REST API organization
 * - All /api/articles/:slug/comments endpoints share same package
 * - Follows REST-first vertical slicing pattern
 *
 * Aggregate Boundary:
 * - Comment is a SEPARATE aggregate from Article (not part of Article aggregate)
 * - Comments can be created/deleted independently of article changes
 * - No transactional consistency required between Article and Comment
 *
 * Why Separate Aggregate:
 * - Comments have independent lifecycle from articles
 * - Different authors can comment on same article (separate ownership)
 * - Comments can be queried/modified without loading entire Article
 * - Performance: avoid loading all comments when fetching article lists
 *
 * Relationship to Article:
 * - Foreign key relationship (articleId) but separate transactional boundary
 * - Cascade delete at database level (comments deleted when article deleted)
 * - No need to load comments when working with Article entity
 *
 * Business Rules:
 * - Only comment author can delete comment
 * - Comment body must not be blank
 */
data class Comment(
    override val id: Long? = null,
    val articleId: Long,
    val authorId: Long,
    val body: String,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
) : Entity<Long> {
    init {
        require(body.isNotBlank()) { "Body must not be blank" }
    }

    override fun withId(newId: Long): Comment = copy(id = newId)

    fun canBeDeletedBy(userId: Long): Boolean = userId == authorId

    fun ensureCanBeDeletedBy(userId: Long) {
        if (!canBeDeletedBy(userId)) {
            throw ForbiddenException("You can only delete your own comments")
        }
    }
}
