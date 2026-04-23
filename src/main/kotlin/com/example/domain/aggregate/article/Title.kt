package com.example.domain.aggregate.article

import com.example.domain.ValueObject

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
