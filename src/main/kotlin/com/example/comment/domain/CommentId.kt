package com.example.comment.domain

import com.example.shared.architecture.ValueObject

@ValueObject
@JvmInline
value class CommentId(
    val value: Long,
)
