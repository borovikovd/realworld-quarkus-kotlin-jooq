package com.example.article.domain

import com.example.shared.architecture.ValueObject

@ValueObject
@JvmInline
value class Title(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Title must not be blank" }
    }

    override fun toString(): String = value
}
