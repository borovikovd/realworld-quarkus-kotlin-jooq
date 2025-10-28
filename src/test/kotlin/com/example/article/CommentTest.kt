package com.example.article

import com.example.shared.exceptions.ForbiddenException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommentTest {
    @Test
    fun `should create valid comment`() {
        val comment =
            Comment(
                articleId = 1L,
                authorId = 2L,
                body = "Test comment body",
            )

        assertEquals(1L, comment.articleId)
        assertEquals(2L, comment.authorId)
        assertEquals("Test comment body", comment.body)
    }

    @Test
    fun `should fail when body is blank`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                Comment(
                    articleId = 1L,
                    authorId = 2L,
                    body = "",
                )
            }

        assertEquals("Body must not be blank", exception.message)
    }

    @Test
    fun `should allow author to delete comment`() {
        val comment =
            Comment(
                articleId = 1L,
                authorId = 2L,
                body = "Test comment body",
            )

        assertTrue(comment.canBeDeletedBy(2L))
    }

    @Test
    fun `should not allow non-author to delete comment`() {
        val comment =
            Comment(
                articleId = 1L,
                authorId = 2L,
                body = "Test comment body",
            )

        assertFalse(comment.canBeDeletedBy(3L))
    }

    @Test
    fun `ensureCanBeDeletedBy should succeed for author`() {
        val comment =
            Comment(
                articleId = 1L,
                authorId = 2L,
                body = "Test comment body",
            )

        comment.ensureCanBeDeletedBy(2L)
    }

    @Test
    fun `ensureCanBeDeletedBy should throw for non-author`() {
        val comment =
            Comment(
                articleId = 1L,
                authorId = 2L,
                body = "Test comment body",
            )

        val exception =
            assertThrows<ForbiddenException> {
                comment.ensureCanBeDeletedBy(3L)
            }

        assertEquals("You can only delete your own comments", exception.message)
    }

    @Test
    fun `should assign new id with withId`() {
        val comment =
            Comment(
                id = 1L,
                articleId = 1L,
                authorId = 2L,
                body = "Test comment body",
            )

        val commentWithNewId = comment.withId(2L)

        assertEquals(2L, commentWithNewId.id)
        assertEquals("Test comment body", commentWithNewId.body)
    }
}
