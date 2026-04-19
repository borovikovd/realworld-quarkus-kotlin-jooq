package com.example.domain.article

import com.example.domain.shared.ValueObject

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
