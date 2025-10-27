package com.example.shared.utils

import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class SlugGenerator {
    fun generateUniqueSlug(
        title: String,
        existingSlugChecker: (String) -> Boolean,
    ): String {
        val baseSlug = generateBaseSlug(title)
        var candidateSlug = baseSlug
        var counter = 2

        while (true) {
            val slugExists = existingSlugChecker(candidateSlug)
            if (!slugExists) {
                return candidateSlug
            }
            candidateSlug = "$baseSlug-$counter"
            counter++
        }
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
