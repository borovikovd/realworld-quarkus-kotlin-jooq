package com.example.domain.aggregate.comment

import com.example.domain.ValueObject

@ValueObject
@JvmInline
value class CommentId(
    val value: Long,
)
