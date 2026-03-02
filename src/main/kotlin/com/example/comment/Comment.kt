package com.example.comment

import com.example.article.ArticleId
import com.example.shared.architecture.AggregateRoot
import com.example.shared.domain.Entity
import com.example.shared.exceptions.ForbiddenException
import com.example.user.UserId
import java.time.OffsetDateTime

@AggregateRoot
class Comment(
    override val id: CommentId,
    val articleId: ArticleId,
    val authorId: UserId,
    val body: String,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
) : Entity<CommentId>() {
    init {
        require(body.isNotBlank()) { "Body must not be blank" }
    }

    fun canBeDeletedBy(userId: UserId): Boolean = userId == authorId

    fun ensureCanBeDeletedBy(userId: UserId) {
        if (!canBeDeletedBy(userId)) {
            throw ForbiddenException("You can only delete your own comments")
        }
    }

    override fun toString(): String = "Comment(id=$id, articleId=$articleId)"
}
