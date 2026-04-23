package com.example.domain.aggregate.article

import com.example.domain.ValueObject

@ValueObject
@JvmInline
value class ArticleId(
    val value: Long,
)
