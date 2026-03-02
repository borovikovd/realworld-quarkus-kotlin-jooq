package com.example.comment

import com.example.shared.architecture.ValueObject

@ValueObject
@JvmInline
value class CommentId(
    val value: Long,
)
