package com.example.article.domain

import com.example.domain.shared.ValueObject

@ValueObject
@JvmInline
value class Tag(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Tag must not be blank" }
        require(!value.any { it.isWhitespace() }) { "Tag must not contain whitespace" }
    }

    override fun toString(): String = value
}
