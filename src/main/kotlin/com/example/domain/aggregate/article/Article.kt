package com.example.domain.aggregate.article

import com.example.domain.AggregateRoot
import com.example.domain.Entity
import com.example.domain.aggregate.user.UserId
import java.time.OffsetDateTime

@AggregateRoot
class Article(
    override val id: ArticleId,
    val slug: Slug,
    val title: Title,
    val description: Description,
    val body: Body,
    val authorId: UserId,
    val tags: Set<Tag> = emptySet(),
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
) : Entity<ArticleId>() {
    fun update(
        slug: Slug,
        title: Title,
        description: Description,
        body: Body,
        updatedAt: OffsetDateTime,
    ): Article =
        Article(
            id = id,
            slug = slug,
            title = title,
            description = description,
            body = body,
            authorId = authorId,
            tags = tags,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    fun canBeDeletedBy(userId: UserId): Boolean = userId == authorId

    override fun toString(): String = "Article(id=$id, slug=$slug, title=$title)"
}
