package com.example.domain.aggregate.user

import com.example.domain.ValueObject

@ValueObject
@JvmInline
value class Username(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Username must not be blank" }
        require(value.length in MIN_LENGTH..MAX_LENGTH) {
            "Username must be between $MIN_LENGTH and $MAX_LENGTH characters"
        }
    }

    override fun toString(): String = value

    companion object {
        internal const val MIN_LENGTH = 3
        internal const val MAX_LENGTH = 50
    }
}
