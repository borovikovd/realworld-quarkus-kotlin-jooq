package com.example.comment.domain

import com.example.shared.architecture.ValueObject

@ValueObject
@JvmInline
value class CommentBody(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Comment body must not be blank" }
    }

    override fun toString(): String = value
}
