package com.example.domain.user

import com.example.domain.shared.ValueObject

@ValueObject
@JvmInline
value class Email(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Email must not be blank" }
        require(EMAIL_REGEX.matches(value)) { "Email must be a valid email address" }
    }

    override fun toString(): String = value

    companion object {
        internal val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    }
}
