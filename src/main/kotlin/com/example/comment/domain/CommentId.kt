package com.example.comment.domain

import com.example.domain.shared.ValueObject

@ValueObject
@JvmInline
value class CommentId(
    val value: Long,
)
