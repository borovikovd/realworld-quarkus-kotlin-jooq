package com.example.application.comment

import com.example.application.profile.ProfileView
import java.time.OffsetDateTime

data class CommentView(
    val id: Long,
    val body: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val author: ProfileView,
)
