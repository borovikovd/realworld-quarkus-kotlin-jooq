package com.example.domain.article

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SlugGeneratorTest {
    

    @Test
    fun `should generate simple slug from title`() {
        val slug =
            SlugGenerator.generateUniqueSlug(
                title = "Hello World",
                existingSlugChecker = { false },
            )

        assertEquals("hello-world", slug.value)
    }

    @Test
    fun `should handle special characters`() {
        val slug =
            SlugGenerator.generateUniqueSlug(
                title = "Hello! World? #Test",
                existingSlugChecker = { false },
            )

        assertEquals("hello-world-test", slug.value)
    }

    @Test
    fun `should handle accented characters`() {
        val slug =
            SlugGenerator.generateUniqueSlug(
                title = "Café Français",
                existingSlugChecker = { false },
            )

        assertEquals("cafe-francais", slug.value)
    }

    @Test
    fun `should collapse multiple spaces`() {
        val slug =
            SlugGenerator.generateUniqueSlug(
                title = "Hello    World    Test",
                existingSlugChecker = { false },
            )

        assertEquals("hello-world-test", slug.value)
    }

    @Test
    fun `should collapse multiple dashes`() {
        val slug =
            SlugGenerator.generateUniqueSlug(
                title = "Hello---World",
                existingSlugChecker = { false },
            )

        assertEquals("hello-world", slug.value)
    }

    @Test
    fun `should trim spaces`() {
        val slug =
            SlugGenerator.generateUniqueSlug(
                title = "  Hello World  ",
                existingSlugChecker = { false },
            )

        assertEquals("hello-world", slug.value)
    }

    @Test
    fun `should generate unique slug with counter when slug exists`() {
        val existingSlugs = mutableSetOf("hello-world")

        val slug =
            SlugGenerator.generateUniqueSlug(
                title = "Hello World",
                existingSlugChecker = { it.value in existingSlugs },
            )

        assertEquals("hello-world-2", slug.value)
    }

    @Test
    fun `should increment counter until unique slug found`() {
        val existingSlugs = mutableSetOf("hello-world", "hello-world-2", "hello-world-3")

        val slug =
            SlugGenerator.generateUniqueSlug(
                title = "Hello World",
                existingSlugChecker = { it.value in existingSlugs },
            )

        assertEquals("hello-world-4", slug.value)
    }

    @Test
    fun `should handle titles with numbers`() {
        val slug =
            SlugGenerator.generateUniqueSlug(
                title = "Kotlin 2.0 Release",
                existingSlugChecker = { false },
            )

        assertEquals("kotlin-20-release", slug.value)
    }

    @Test
    fun `should handle mixed case`() {
        val slug =
            SlugGenerator.generateUniqueSlug(
                title = "MiXeD CaSe TiTlE",
                existingSlugChecker = { false },
            )

        assertEquals("mixed-case-title", slug.value)
    }

    @Test
    fun `should handle emoji and unicode`() {
        val slug =
            SlugGenerator.generateUniqueSlug(
                title = "Hello 🌍 World",
                existingSlugChecker = { false },
            )

        assertEquals("hello-world", slug.value)
    }

    @Test
    fun `should throw exception when max attempts exhausted`() {
        val exception =
            assertThrows<IllegalStateException> {
                SlugGenerator.generateUniqueSlug(
                    title = "Hello World",
                    existingSlugChecker = { true },
                )
            }
        assertTrue(exception.message!!.contains("Could not generate unique slug"))
    }
}
