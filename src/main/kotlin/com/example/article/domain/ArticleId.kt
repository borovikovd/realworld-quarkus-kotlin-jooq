package com.example.article.domain

import com.example.domain.shared.ValueObject

@ValueObject
@JvmInline
value class ArticleId(
    val value: Long,
)
