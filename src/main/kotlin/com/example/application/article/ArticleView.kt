package com.example.application.article

import com.example.application.profile.ProfileView
import java.time.OffsetDateTime

data class ArticleView(
    val slug: String,
    val title: String,
    val description: String,
    val body: String,
    val tagList: List<String>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val favorited: Boolean,
    val favoritesCount: Int,
    val author: ProfileView,
)
