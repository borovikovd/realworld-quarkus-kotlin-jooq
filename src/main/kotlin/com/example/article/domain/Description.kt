package com.example.article.domain

import com.example.shared.architecture.ValueObject

@ValueObject
@JvmInline
value class Description(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Description must not be blank" }
    }

    override fun toString(): String = value
}
