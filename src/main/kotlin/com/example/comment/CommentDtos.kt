package com.example.comment

import com.example.article.ArticleId
import com.example.user.ProfileDto
import com.example.user.UserId
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
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

data class CommentDto(
    val id: Long,
    val body: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val author: ProfileDto,
)

data class NewCommentRequest(
    @field:Valid val comment: NewComment,
)

data class NewComment(
    @field:NotBlank val body: String,
)

data class CommentEnvelope(
    val comment: CommentDto,
)

data class CommentListEnvelope(
    val comments: List<CommentDto>,
)
