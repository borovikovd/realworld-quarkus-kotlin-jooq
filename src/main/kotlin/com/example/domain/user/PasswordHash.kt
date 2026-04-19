package com.example.domain.user

import com.example.domain.shared.ValueObject

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
