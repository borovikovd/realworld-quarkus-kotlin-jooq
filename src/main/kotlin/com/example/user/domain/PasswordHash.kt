package com.example.user.domain

import com.example.shared.architecture.ValueObject

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
