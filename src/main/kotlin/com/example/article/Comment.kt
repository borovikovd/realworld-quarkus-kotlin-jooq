package com.example.article

import com.example.shared.domain.Entity
import com.example.shared.exceptions.ForbiddenException
import java.time.OffsetDateTime

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
