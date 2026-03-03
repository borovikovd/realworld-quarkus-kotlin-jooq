package com.example.comment.application

import com.example.profile.application.ProfileSummary
import java.time.OffsetDateTime

data class CommentSummary(
    val id: Long,
    val body: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val author: ProfileSummary,
)
