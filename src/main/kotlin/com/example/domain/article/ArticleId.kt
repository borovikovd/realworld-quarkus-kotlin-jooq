package com.example.domain.article

import com.example.domain.shared.ValueObject

@ValueObject
@JvmInline
value class ArticleId(
    val value: Long,
)
