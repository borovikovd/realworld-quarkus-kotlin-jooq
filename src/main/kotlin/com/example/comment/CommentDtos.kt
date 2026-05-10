package com.example.comment

import com.example.user.ProfileDto
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

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
    @field:NotBlank @field:Size(max = 65536) val body: String,
)

data class CommentEnvelope(
    val comment: CommentDto,
)

data class CommentListEnvelope(
    val comments: List<CommentDto>,
)
