package com.example.comment.domain

import com.example.article.domain.ArticleId
import com.example.shared.architecture.AggregateRoot
import com.example.shared.domain.Entity
import com.example.user.domain.UserId
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

    override fun toString(): String = "Comment(id=$id, articleId=$articleId)"
}
