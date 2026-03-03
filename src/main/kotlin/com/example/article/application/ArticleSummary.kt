package com.example.article.application

import com.example.profile.application.ProfileSummary
import java.time.OffsetDateTime

data class ArticleSummary(
    val slug: String,
    val title: String,
    val description: String,
    val body: String,
    val tagList: List<String>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val favorited: Boolean,
    val favoritesCount: Int,
    val author: ProfileSummary,
)
