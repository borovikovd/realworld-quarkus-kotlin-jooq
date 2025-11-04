package com.example.user

@JvmInline
value class Email(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Email cannot be blank" }
        require(value.contains("@")) { "Email must contain @" }
        require(value.length <= MAX_LENGTH) { "Email must be at most $MAX_LENGTH characters" }
    }

    companion object {
        private const val MAX_LENGTH = 255

        fun of(value: String): Email = Email(value.trim().lowercase())
    }
}
