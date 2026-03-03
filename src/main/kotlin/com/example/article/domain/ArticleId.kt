package com.example.article.domain

import com.example.shared.architecture.ValueObject

@ValueObject
@JvmInline
value class ArticleId(
    val value: Long,
)
