package com.example.application.port.outbound

import java.time.OffsetDateTime

data class ArticleReadModel(
    val slug: String,
    val title: String,
    val description: String,
    val body: String,
    val tagList: List<String>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val favorited: Boolean,
    val favoritesCount: Int,
    val author: ProfileReadModel,
)
