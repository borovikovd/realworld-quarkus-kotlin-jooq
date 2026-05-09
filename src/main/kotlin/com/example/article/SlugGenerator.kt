package com.example.article

import java.text.Normalizer

object SlugGenerator {
    private const val MAX_SLUG_ATTEMPTS = 100

    fun generateUniqueSlug(
        title: String,
        existingSlugChecker: (String) -> Boolean,
    ): String {
        val baseSlug = generateBaseSlug(title)
        var candidate = baseSlug
        var counter = 2

        repeat(MAX_SLUG_ATTEMPTS) {
            if (!existingSlugChecker(candidate)) return candidate
            candidate = "$baseSlug-$counter"
            counter++
        }

        error("Could not generate unique slug after $MAX_SLUG_ATTEMPTS attempts")
    }

    private fun generateBaseSlug(title: String): String {
        val normalized = Normalizer.normalize(title, Normalizer.Form.NFD)
        return normalized
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
            .replace("[^a-z0-9\\s-]".toRegex(), "")
            .trim()
            .replace("\\s+".toRegex(), "-")
            .replace("-+".toRegex(), "-")
    }
}
