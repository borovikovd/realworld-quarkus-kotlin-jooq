package com.example.domain.comment

import com.example.domain.article.ArticleId
import com.example.domain.shared.AggregateRoot
import com.example.domain.shared.Entity
import com.example.domain.user.UserId
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
