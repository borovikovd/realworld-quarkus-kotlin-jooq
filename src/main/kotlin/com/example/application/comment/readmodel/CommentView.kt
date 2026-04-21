package com.example.application.comment.readmodel

import com.example.application.profile.readmodel.ProfileView
import java.time.OffsetDateTime

data class CommentView(
    val id: Long,
    val body: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val author: ProfileView,
)
