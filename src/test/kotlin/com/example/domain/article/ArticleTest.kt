package com.example.domain.article

import com.example.domain.user.UserId
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
                id = ArticleId(1L),
                slug = Slug("test-article"),
                title = "Test Article",
                description = "Test description",
                body = "Test body",
                authorId = UserId(1L),
                tags = setOf("tag1", "tag2"),
            )

        assertEquals("test-article", article.slug.value)
        assertEquals("Test Article", article.title)
        assertEquals("Test description", article.description)
        assertEquals("Test body", article.body)
        assertEquals(UserId(1L), article.authorId)
        assertEquals(setOf("tag1", "tag2"), article.tags)
    }

    @Test
    fun `should fail when title is blank`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                Article(
                    id = ArticleId(1L),
                    slug = Slug("test-article"),
                    title = "",
                    description = "Test description",
                    body = "Test body",
                    authorId = UserId(1L),
                )
            }

        assertEquals("Title must not be blank", exception.message)
    }

    @Test
    fun `should fail when description is blank`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                Article(
                    id = ArticleId(1L),
                    slug = Slug("test-article"),
                    title = "Test Article",
                    description = "",
                    body = "Test body",
                    authorId = UserId(1L),
                )
            }

        assertEquals("Description must not be blank", exception.message)
    }

    @Test
    fun `should fail when body is blank`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                Article(
                    id = ArticleId(1L),
                    slug = Slug("test-article"),
                    title = "Test Article",
                    description = "Test description",
                    body = "",
                    authorId = UserId(1L),
                )
            }

        assertEquals("Body must not be blank", exception.message)
    }

    @Test
    fun `should update article fields and timestamp`() {
        val originalArticle =
            Article(
                id = ArticleId(1L),
                slug = Slug("original-slug"),
                title = "Original Title",
                description = "Original description",
                body = "Original body",
                authorId = UserId(1L),
                createdAt = OffsetDateTime.now().minusDays(1),
                updatedAt = OffsetDateTime.now().minusDays(1),
            )

        val updatedArticle =
            originalArticle.update(
                slug = Slug("updated-slug"),
                title = "Updated Title",
                description = "Updated description",
                body = "Updated body",
                updatedAt = OffsetDateTime.now(),
            )

        assertEquals("updated-slug", updatedArticle.slug.value)
        assertEquals("Updated Title", updatedArticle.title)
        assertEquals("Updated description", updatedArticle.description)
        assertEquals("Updated body", updatedArticle.body)
        assertTrue(updatedArticle.updatedAt.isAfter(originalArticle.updatedAt))
    }

    @Test
    fun `should allow author to delete article`() {
        val article =
            Article(
                id = ArticleId(1L),
                slug = Slug("test-article"),
                title = "Test Article",
                description = "Test description",
                body = "Test body",
                authorId = UserId(1L),
            )

        assertTrue(article.canBeDeletedBy(UserId(1L)))
    }

    @Test
    fun `should not allow non-author to delete article`() {
        val article =
            Article(
                id = ArticleId(1L),
                slug = Slug("test-article"),
                title = "Test Article",
                description = "Test description",
                body = "Test body",
                authorId = UserId(1L),
            )

        assertFalse(article.canBeDeletedBy(UserId(2L)))
    }

    @Test
    fun `should have identity-based equality`() {
        val article1 =
            Article(
                id = ArticleId(1L),
                slug = Slug("slug-1"),
                title = "Title 1",
                description = "Desc 1",
                body = "Body 1",
                authorId = UserId(1L),
            )

        val article2 =
            Article(
                id = ArticleId(1L),
                slug = Slug("slug-2"),
                title = "Title 2",
                description = "Desc 2",
                body = "Body 2",
                authorId = UserId(2L),
            )

        assertEquals(article1, article2)
        assertEquals(article1.hashCode(), article2.hashCode())
    }
}
