package com.example.article

import com.example.shared.architecture.ValueObject

@ValueObject
@JvmInline
value class ArticleId(
    val value: Long,
)
