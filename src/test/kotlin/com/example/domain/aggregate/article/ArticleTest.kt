package com.example.domain.aggregate.article

import com.example.domain.aggregate.user.UserId
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
                title = Title("Test Article"),
                description = Description("Test description"),
                body = Body("Test body"),
                authorId = UserId(1L),
                tags = setOf(Tag("tag1"), Tag("tag2")),
            )

        assertEquals("test-article", article.slug.value)
        assertEquals("Test Article", article.title.value)
        assertEquals("Test description", article.description.value)
        assertEquals("Test body", article.body.value)
        assertEquals(UserId(1L), article.authorId)
        assertEquals(setOf(Tag("tag1"), Tag("tag2")), article.tags)
    }

    @Test
    fun `should fail when title is blank`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                Article(
                    id = ArticleId(1L),
                    slug = Slug("test-article"),
                    title = Title(""),
                    description = Description("Test description"),
                    body = Body("Test body"),
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
                    title = Title("Test Article"),
                    description = Description(""),
                    body = Body("Test body"),
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
                    title = Title("Test Article"),
                    description = Description("Test description"),
                    body = Body(""),
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
                title = Title("Original Title"),
                description = Description("Original description"),
                body = Body("Original body"),
                authorId = UserId(1L),
                createdAt = OffsetDateTime.now().minusDays(1),
                updatedAt = OffsetDateTime.now().minusDays(1),
            )

        val updatedArticle =
            originalArticle.update(
                slug = Slug("updated-slug"),
                title = Title("Updated Title"),
                description = Description("Updated description"),
                body = Body("Updated body"),
                updatedAt = OffsetDateTime.now(),
            )

        assertEquals("updated-slug", updatedArticle.slug.value)
        assertEquals("Updated Title", updatedArticle.title.value)
        assertEquals("Updated description", updatedArticle.description.value)
        assertEquals("Updated body", updatedArticle.body.value)
        assertTrue(updatedArticle.updatedAt.isAfter(originalArticle.updatedAt))
    }

    @Test
    fun `should allow author to delete article`() {
        val article =
            Article(
                id = ArticleId(1L),
                slug = Slug("test-article"),
                title = Title("Test Article"),
                description = Description("Test description"),
                body = Body("Test body"),
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
                title = Title("Test Article"),
                description = Description("Test description"),
                body = Body("Test body"),
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
                title = Title("Title 1"),
                description = Description("Desc 1"),
                body = Body("Body 1"),
                authorId = UserId(1L),
            )

        val article2 =
            Article(
                id = ArticleId(1L),
                slug = Slug("slug-2"),
                title = Title("Title 2"),
                description = Description("Desc 2"),
                body = Body("Body 2"),
                authorId = UserId(2L),
            )

        assertEquals(article1, article2)
        assertEquals(article1.hashCode(), article2.hashCode())
    }
}
