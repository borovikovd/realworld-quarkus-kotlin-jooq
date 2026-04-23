package com.example.domain.aggregate.article

import com.example.domain.ValueObject

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
