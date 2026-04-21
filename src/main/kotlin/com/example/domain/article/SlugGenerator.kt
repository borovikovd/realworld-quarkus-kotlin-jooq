package com.example.domain.article

object SlugGenerator {
    private const val MAX_SLUG_ATTEMPTS = 100

    fun generateUniqueSlug(
        title: String,
        existingSlugChecker: (Slug) -> Boolean,
    ): Slug {
        val baseSlug = generateBaseSlug(title)
        var candidateSlug = baseSlug
        var counter = 2

        repeat(MAX_SLUG_ATTEMPTS) {
            val slug = Slug(candidateSlug)
            if (!existingSlugChecker(slug)) {
                return slug
            }
            candidateSlug = "$baseSlug-$counter"
            counter++
        }

        error("Could not generate unique slug after $MAX_SLUG_ATTEMPTS attempts")
    }

    private fun generateBaseSlug(title: String): String {
        val normalized = java.text.Normalizer.normalize(title, java.text.Normalizer.Form.NFD)
        return normalized
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
            .replace("[^a-z0-9\\s-]".toRegex(), "")
            .trim()
            .replace("\\s+".toRegex(), "-")
            .replace("-+".toRegex(), "-")
    }
}
