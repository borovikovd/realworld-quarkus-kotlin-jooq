package com.example.domain.aggregate.user

import com.example.domain.ValueObject

@ValueObject
@JvmInline
value class PasswordHash(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Password hash must not be blank" }
    }

    override fun toString(): String = "PasswordHash(***)"
}
