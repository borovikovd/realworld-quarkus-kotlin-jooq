package com.example.domain.aggregate.comment

import com.example.domain.AggregateRoot
import com.example.domain.Entity
import com.example.domain.aggregate.article.ArticleId
import com.example.domain.aggregate.user.UserId
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
