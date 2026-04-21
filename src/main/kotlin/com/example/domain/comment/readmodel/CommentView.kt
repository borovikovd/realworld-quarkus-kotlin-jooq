package com.example.domain.comment.readmodel

import com.example.domain.profile.readmodel.ProfileView
import java.time.OffsetDateTime

data class CommentView(
    val id: Long,
    val body: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val author: ProfileView,
)
