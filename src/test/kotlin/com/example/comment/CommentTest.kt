package com.example.comment

import com.example.article.ArticleId
import com.example.user.UserId
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
                id = CommentId(1L),
                articleId = ArticleId(1L),
                authorId = UserId(2L),
                body = "Test comment body",
            )

        assertEquals(ArticleId(1L), comment.articleId)
        assertEquals(UserId(2L), comment.authorId)
        assertEquals("Test comment body", comment.body)
    }

    @Test
    fun `should fail when body is blank`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                Comment(
                    id = CommentId(1L),
                    articleId = ArticleId(1L),
                    authorId = UserId(2L),
                    body = "",
                )
            }

        assertEquals("Body must not be blank", exception.message)
    }

    @Test
    fun `should allow author to delete comment`() {
        val comment =
            Comment(
                id = CommentId(1L),
                articleId = ArticleId(1L),
                authorId = UserId(2L),
                body = "Test comment body",
            )

        assertTrue(comment.canBeDeletedBy(UserId(2L)))
    }

    @Test
    fun `should not allow non-author to delete comment`() {
        val comment =
            Comment(
                id = CommentId(1L),
                articleId = ArticleId(1L),
                authorId = UserId(2L),
                body = "Test comment body",
            )

        assertFalse(comment.canBeDeletedBy(UserId(3L)))
    }

    @Test
    fun `should have identity-based equality`() {
        val comment1 =
            Comment(
                id = CommentId(1L),
                articleId = ArticleId(1L),
                authorId = UserId(2L),
                body = "Body 1",
            )

        val comment2 =
            Comment(
                id = CommentId(1L),
                articleId = ArticleId(2L),
                authorId = UserId(3L),
                body = "Body 2",
            )

        assertEquals(comment1, comment2)
        assertEquals(comment1.hashCode(), comment2.hashCode())
    }
}
