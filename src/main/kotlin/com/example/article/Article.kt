package com.example.article

import com.example.shared.architecture.AggregateRoot
import com.example.shared.domain.Entity
import java.time.OffsetDateTime

@AggregateRoot
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
