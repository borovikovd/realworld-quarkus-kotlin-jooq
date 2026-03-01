package com.example.comment

import com.example.article.Article
import com.example.article.ArticleRepository
import com.example.shared.exceptions.ForbiddenException
import com.example.shared.exceptions.NotFoundException
import com.example.shared.security.SecurityContext
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

    private val userId = 1L
    private val articleId = 10L
    private val slug = "test-article"
    private val article = Article(
        id = articleId,
        slug = slug,
        title = "Test Article",
        description = "desc",
        body = "body",
        authorId = 2L,
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
    fun `addComment should create comment and return CommentId`() {
        val body = "Great article!"
        val createdComment = Comment(id = 1L, articleId = articleId, authorId = userId, body = body)

        every { securityContext.requireCurrentUserId() } returns userId
        every { articleRepository.findBySlug(slug) } returns article
        every { commentRepository.create(any()) } returns createdComment

        val result = commentService.addComment(slug, body)

        assertEquals(CommentId(1L), result)
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
        val commentId = 5L
        val comment = Comment(id = commentId, articleId = articleId, authorId = userId, body = "My comment")

        every { securityContext.requireCurrentUserId() } returns userId
        every { articleRepository.findBySlug(slug) } returns article
        every { commentRepository.findById(commentId) } returns comment
        every { commentRepository.deleteById(commentId) } returns Unit

        commentService.deleteComment(slug, commentId)

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
        every { commentRepository.findById(5L) } returns null

        assertThrows<NotFoundException> {
            commentService.deleteComment(slug, 5L)
        }
    }

    @Test
    fun `deleteComment should throw NotFoundException when comment belongs to different article`() {
        val commentId = 5L
        val comment = Comment(id = commentId, articleId = 999L, authorId = userId, body = "Wrong article")

        every { securityContext.requireCurrentUserId() } returns userId
        every { articleRepository.findBySlug(slug) } returns article
        every { commentRepository.findById(commentId) } returns comment

        assertThrows<NotFoundException> {
            commentService.deleteComment(slug, commentId)
        }
    }

    @Test
    fun `deleteComment should throw ForbiddenException when user is not author`() {
        val commentId = 5L
        val differentUserId = 99L
        val comment = Comment(id = commentId, articleId = articleId, authorId = differentUserId, body = "Not my comment")

        every { securityContext.requireCurrentUserId() } returns userId
        every { articleRepository.findBySlug(slug) } returns article
        every { commentRepository.findById(commentId) } returns comment

        assertThrows<ForbiddenException> {
            commentService.deleteComment(slug, commentId)
        }
    }
}
