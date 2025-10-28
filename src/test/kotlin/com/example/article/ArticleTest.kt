package com.example.article

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArticleTest {
    @Test
    fun `should create valid article`() {
        val article =
            Article(
                slug = "test-article",
                title = "Test Article",
                description = "Test description",
                body = "Test body",
                authorId = 1L,
                tags = setOf("tag1", "tag2"),
            )

        assertEquals("test-article", article.slug)
        assertEquals("Test Article", article.title)
        assertEquals("Test description", article.description)
        assertEquals("Test body", article.body)
        assertEquals(1L, article.authorId)
        assertEquals(setOf("tag1", "tag2"), article.tags)
    }

    @Test
    fun `should fail when title is blank`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                Article(
                    slug = "test-article",
                    title = "",
                    description = "Test description",
                    body = "Test body",
                    authorId = 1L,
                )
            }

        assertEquals("Title must not be blank", exception.message)
    }

    @Test
    fun `should fail when description is blank`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                Article(
                    slug = "test-article",
                    title = "Test Article",
                    description = "",
                    body = "Test body",
                    authorId = 1L,
                )
            }

        assertEquals("Description must not be blank", exception.message)
    }

    @Test
    fun `should fail when body is blank`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                Article(
                    slug = "test-article",
                    title = "Test Article",
                    description = "Test description",
                    body = "",
                    authorId = 1L,
                )
            }

        assertEquals("Body must not be blank", exception.message)
    }

    @Test
    fun `should update article fields and timestamp`() {
        val originalArticle =
            Article(
                slug = "original-slug",
                title = "Original Title",
                description = "Original description",
                body = "Original body",
                authorId = 1L,
                createdAt = OffsetDateTime.now().minusDays(1),
                updatedAt = OffsetDateTime.now().minusDays(1),
            )

        val updatedArticle =
            originalArticle.update(
                slug = "updated-slug",
                title = "Updated Title",
                description = "Updated description",
                body = "Updated body",
            )

        assertEquals("updated-slug", updatedArticle.slug)
        assertEquals("Updated Title", updatedArticle.title)
        assertEquals("Updated description", updatedArticle.description)
        assertEquals("Updated body", updatedArticle.body)
        assertTrue(updatedArticle.updatedAt.isAfter(originalArticle.updatedAt))
    }

    @Test
    fun `should allow author to delete article`() {
        val article =
            Article(
                slug = "test-article",
                title = "Test Article",
                description = "Test description",
                body = "Test body",
                authorId = 1L,
            )

        assertTrue(article.canBeDeletedBy(1L))
    }

    @Test
    fun `should not allow non-author to delete article`() {
        val article =
            Article(
                slug = "test-article",
                title = "Test Article",
                description = "Test description",
                body = "Test body",
                authorId = 1L,
            )

        assertFalse(article.canBeDeletedBy(2L))
    }

    @Test
    fun `should assign new id with withId`() {
        val article =
            Article(
                id = 1L,
                slug = "test-article",
                title = "Test Article",
                description = "Test description",
                body = "Test body",
                authorId = 1L,
            )

        val articleWithNewId = article.withId(2L)

        assertEquals(2L, articleWithNewId.id)
        assertEquals("test-article", articleWithNewId.slug)
    }
}
