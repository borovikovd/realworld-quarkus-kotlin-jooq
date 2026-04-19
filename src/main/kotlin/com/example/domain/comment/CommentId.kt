package com.example.domain.comment

import com.example.domain.shared.ValueObject

@ValueObject
@JvmInline
value class CommentId(
    val value: Long,
)
