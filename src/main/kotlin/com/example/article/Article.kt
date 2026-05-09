package com.example.article

import com.example.user.UserId
import java.time.OffsetDateTime

@JvmInline
value class ArticleId(
    val value: Long,
)

data class Article(
    val id: ArticleId,
    val slug: String,
    val title: String,
    val description: String,
    val body: String,
    val authorId: UserId,
    val tags: Set<String> = emptySet(),
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

data class ArticleFilter(
    val tag: String?,
    val author: String?,
    val favorited: String?,
)

data class Page(
    val limit: Int,
    val offset: Int,
)
