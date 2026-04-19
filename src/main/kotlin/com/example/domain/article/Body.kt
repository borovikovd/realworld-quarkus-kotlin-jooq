package com.example.domain.article

import com.example.domain.shared.ValueObject

@ValueObject
@JvmInline
value class Body(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Body must not be blank" }
    }

    override fun toString(): String = value
}
