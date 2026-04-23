package com.example.domain.aggregate.article

import com.example.domain.ValueObject

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
