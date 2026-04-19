package com.example.domain.article

import com.example.domain.shared.AggregateRoot
import com.example.domain.shared.Entity
import com.example.domain.user.UserId
import java.time.OffsetDateTime

@AggregateRoot
class Article(
    override val id: ArticleId,
    val slug: String,
    val title: String,
    val description: String,
    val body: String,
    val authorId: UserId,
    val tags: Set<String> = emptySet(),
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
) : Entity<ArticleId>() {
    init {
        require(title.isNotBlank()) { "Title must not be blank" }
        require(description.isNotBlank()) { "Description must not be blank" }
        require(body.isNotBlank()) { "Body must not be blank" }
    }

    fun update(
        slug: String,
        title: String,
        description: String,
        body: String,
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
            updatedAt = OffsetDateTime.now(),
        )

    fun canBeDeletedBy(userId: UserId): Boolean = userId == authorId

    override fun toString(): String = "Article(id=$id, slug=$slug, title=$title)"
}
