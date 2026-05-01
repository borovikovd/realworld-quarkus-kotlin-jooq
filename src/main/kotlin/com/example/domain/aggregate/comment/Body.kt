package com.example.domain.aggregate.comment

import com.example.domain.ValueObject

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
