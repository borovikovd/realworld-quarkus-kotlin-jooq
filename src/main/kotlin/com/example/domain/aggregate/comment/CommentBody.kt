package com.example.domain.aggregate.comment

import com.example.domain.ValueObject

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
