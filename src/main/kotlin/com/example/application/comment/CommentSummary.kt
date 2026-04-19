package com.example.application.comment

import com.example.application.profile.ProfileSummary
import java.time.OffsetDateTime

data class CommentSummary(
    val id: Long,
    val body: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val author: ProfileSummary,
)
