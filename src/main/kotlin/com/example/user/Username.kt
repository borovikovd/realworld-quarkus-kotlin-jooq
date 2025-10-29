package com.example.user

@JvmInline
value class Username(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Username cannot be blank" }
        require(value.length >= MIN_LENGTH) { "Username must be at least $MIN_LENGTH characters" }
        require(value.length <= MAX_LENGTH) { "Username must be at most $MAX_LENGTH characters" }
        require(value.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            "Username can only contain letters, numbers, underscores, and hyphens"
        }
    }

    companion object {
        private const val MIN_LENGTH = 3
        private const val MAX_LENGTH = 255

        fun of(value: String): Username = Username(value.trim())
    }
}
