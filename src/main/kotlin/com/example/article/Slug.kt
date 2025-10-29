package com.example.article

@JvmInline
value class Slug(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Slug cannot be blank" }
        require(value.length <= MAX_LENGTH) { "Slug must be at most $MAX_LENGTH characters" }
        require(value.matches(Regex("^[a-z0-9-]+$"))) {
            "Slug can only contain lowercase letters, numbers, and hyphens"
        }
    }

    companion object {
        private const val MAX_LENGTH = 255

        fun of(title: String): Slug {
            val slug =
                title
                    .trim()
                    .lowercase()
                    .replace(Regex("[^a-z0-9\\s-]"), "")
                    .replace(Regex("\\s+"), "-")
                    .replace(Regex("-+"), "-")
                    .trim('-')
            return Slug(slug)
        }

        fun from(value: String): Slug = Slug(value)
    }
}
