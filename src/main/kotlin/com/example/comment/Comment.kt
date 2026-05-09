package com.example.comment

import com.example.article.ArticleId
import com.example.user.UserId
import java.time.OffsetDateTime

@JvmInline
value class CommentId(
    val value: Long,
)

data class Comment(
    val id: CommentId,
    val articleId: ArticleId,
    val authorId: UserId,
    val body: String,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
