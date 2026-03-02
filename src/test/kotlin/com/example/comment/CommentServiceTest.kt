package com.example.comment

import com.example.article.Article
import com.example.article.ArticleId
import com.example.article.ArticleRepository
import com.example.shared.exceptions.ForbiddenException
import com.example.shared.exceptions.NotFoundException
import com.example.shared.exceptions.ValidationException
import com.example.shared.security.SecurityContext
import com.example.user.UserId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class CommentServiceTest {
    private lateinit var commentService: CommentService
    private lateinit var commentRepository: CommentRepository
    private lateinit var articleRepository: ArticleRepository
    private lateinit var securityContext: SecurityContext

    private val userId = UserId(1L)
    private val articleId = ArticleId(10L)
    private val slug = "test-article"
    private val article = Article(
        id = articleId,
        slug = slug,
        title = "Test Article",
        description = "desc",
        body = "body",
        authorId = UserId(2L),
    )

    @BeforeEach
    fun setup() {
        commentRepository = mockk()
        articleRepository = mockk()
        securityContext = mockk()
        commentService = CommentService(
            commentRepository = commentRepository,
            articleRepository = articleRepository,
            securityContext = securityContext,
        )
    }

    @Test
    fun `addComment should throw ValidationException when body is blank`() {
        val exception =
            assertThrows<ValidationException> {
                commentService.addComment(slug, "")
            }

        assertEquals(listOf("must not be blank"), exception.errors["body"])
    }

    @Test
    fun `addComment should create comment and return CommentId`() {
        val body = "Great article!"
        val commentId = CommentId(1L)

        every { securityContext.requireCurrentUserId() } returns userId
        every { articleRepository.findBySlug(slug) } returns article
        every { commentRepository.nextId() } returns commentId
        every { commentRepository.create(any()) } answers { firstArg() }

        val result = commentService.addComment(slug, body)

        assertEquals(commentId, result)
        verify { commentRepository.nextId() }
        verify { commentRepository.create(match { it.articleId == articleId && it.authorId == userId && it.body == body }) }
    }

    @Test
    fun `addComment should throw NotFoundException when article not found`() {
        every { securityContext.requireCurrentUserId() } returns userId
        every { articleRepository.findBySlug(slug) } returns null

        assertThrows<NotFoundException> {
            commentService.addComment(slug, "comment body")
        }
    }

    @Test
    fun `deleteComment should delete when user is author`() {
        val commentId = CommentId(5L)
        val comment = Comment(id = commentId, articleId = articleId, authorId = userId, body = "My comment")

        every { securityContext.requireCurrentUserId() } returns userId
        every { articleRepository.findBySlug(slug) } returns article
        every { commentRepository.findById(commentId) } returns comment
        every { commentRepository.deleteById(commentId) } returns Unit

        commentService.deleteComment(slug, 5L)

        verify { commentRepository.deleteById(commentId) }
    }

    @Test
    fun `deleteComment should throw NotFoundException when article not found`() {
        every { securityContext.requireCurrentUserId() } returns userId
        every { articleRepository.findBySlug(slug) } returns null

        assertThrows<NotFoundException> {
            commentService.deleteComment(slug, 5L)
        }
    }

    @Test
    fun `deleteComment should throw NotFoundException when comment not found`() {
        every { securityContext.requireCurrentUserId() } returns userId
        every { articleRepository.findBySlug(slug) } returns article
        every { commentRepository.findById(CommentId(5L)) } returns null

        assertThrows<NotFoundException> {
            commentService.deleteComment(slug, 5L)
        }
    }

    @Test
    fun `deleteComment should throw NotFoundException when comment belongs to different article`() {
        val commentId = CommentId(5L)
        val comment = Comment(id = commentId, articleId = ArticleId(999L), authorId = userId, body = "Wrong article")

        every { securityContext.requireCurrentUserId() } returns userId
        every { articleRepository.findBySlug(slug) } returns article
        every { commentRepository.findById(commentId) } returns comment

        assertThrows<NotFoundException> {
            commentService.deleteComment(slug, 5L)
        }
    }

    @Test
    fun `deleteComment should throw ForbiddenException when user is not author`() {
        val commentId = CommentId(5L)
        val differentUserId = UserId(99L)
        val comment = Comment(id = commentId, articleId = articleId, authorId = differentUserId, body = "Not my comment")

        every { securityContext.requireCurrentUserId() } returns userId
        every { articleRepository.findBySlug(slug) } returns article
        every { commentRepository.findById(commentId) } returns comment

        assertThrows<ForbiddenException> {
            commentService.deleteComment(slug, 5L)
        }
    }
}
