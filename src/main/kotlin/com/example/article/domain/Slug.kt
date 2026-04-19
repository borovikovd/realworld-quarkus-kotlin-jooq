package com.example.article.domain

import com.example.shared.architecture.ValueObject

@ValueObject
@JvmInline
value class Slug(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Slug must not be blank" }
    }

    override fun toString(): String = value
}
