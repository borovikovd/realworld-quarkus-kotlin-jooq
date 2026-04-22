package com.example.application.command

import com.example.domain.article.Article
import com.example.domain.article.ArticleId
import com.example.domain.article.ArticleRepository
import com.example.domain.article.Slug
import com.example.domain.comment.Comment
import com.example.domain.comment.CommentId
import com.example.domain.comment.CommentRepository
import com.example.domain.shared.ForbiddenException
import com.example.domain.shared.NotFoundException
import com.example.domain.shared.ValidationException
import com.example.application.CurrentUser
import com.example.domain.user.UserId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class CommentCommandsTest {
    private lateinit var commentCommands: CommentCommands
    private lateinit var commentRepository: CommentRepository
    private lateinit var articleRepository: ArticleRepository
    private lateinit var currentUser: CurrentUser

    private val userId = UserId(1L)
    private val articleId = ArticleId(10L)
    private val slug = Slug("test-article")
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
        currentUser = mockk()
        commentCommands = CommentCommands(
            commentRepository = commentRepository,
            articleRepository = articleRepository,
            currentUser = currentUser,
        )
    }

    @Test
    fun `addComment should throw ValidationException when body is blank`() {
        val exception =
            assertThrows<ValidationException> {
                commentCommands.addComment(slug.value, "")
            }

        assertEquals(listOf("must not be blank"), exception.errors["body"])
    }

    @Test
    fun `addComment should create comment and return comment id`() {
        val body = "Great article!"
        val commentId = CommentId(1L)

        every { currentUser.require() } returns userId
        every { articleRepository.findBySlug(slug) } returns article
        every { commentRepository.nextId() } returns commentId
        every { commentRepository.create(any()) } answers { firstArg() }

        val result = commentCommands.addComment(slug.value, body)

        assertEquals(commentId.value, result)
        verify { commentRepository.nextId() }
        verify { commentRepository.create(match { it.articleId == articleId && it.authorId == userId && it.body == body }) }
    }

    @Test
    fun `addComment should throw NotFoundException when article not found`() {
        every { currentUser.require() } returns userId
        every { articleRepository.findBySlug(slug) } returns null

        assertThrows<NotFoundException> {
            commentCommands.addComment(slug.value, "comment body")
        }
    }

    @Test
    fun `deleteComment should delete when user is author`() {
        val commentId = CommentId(5L)
        val comment = Comment(id = commentId, articleId = articleId, authorId = userId, body = "My comment")

        every { currentUser.require() } returns userId
        every { articleRepository.findBySlug(slug) } returns article
        every { commentRepository.findById(commentId) } returns comment
        every { commentRepository.deleteById(commentId) } returns Unit

        commentCommands.deleteComment(slug.value, 5L)

        verify { commentRepository.deleteById(commentId) }
    }

    @Test
    fun `deleteComment should throw NotFoundException when article not found`() {
        every { currentUser.require() } returns userId
        every { articleRepository.findBySlug(slug) } returns null

        assertThrows<NotFoundException> {
            commentCommands.deleteComment(slug.value, 5L)
        }
    }

    @Test
    fun `deleteComment should throw NotFoundException when comment not found`() {
        every { currentUser.require() } returns userId
        every { articleRepository.findBySlug(slug) } returns article
        every { commentRepository.findById(CommentId(5L)) } returns null

        assertThrows<NotFoundException> {
            commentCommands.deleteComment(slug.value, 5L)
        }
    }

    @Test
    fun `deleteComment should throw NotFoundException when comment belongs to different article`() {
        val commentId = CommentId(5L)
        val comment = Comment(id = commentId, articleId = ArticleId(999L), authorId = userId, body = "Wrong article")

        every { currentUser.require() } returns userId
        every { articleRepository.findBySlug(slug) } returns article
        every { commentRepository.findById(commentId) } returns comment

        assertThrows<NotFoundException> {
            commentCommands.deleteComment(slug.value, 5L)
        }
    }

    @Test
    fun `deleteComment should throw ForbiddenException when user is not author`() {
        val commentId = CommentId(5L)
        val differentUserId = UserId(99L)
        val comment = Comment(id = commentId, articleId = articleId, authorId = differentUserId, body = "Not my comment")

        every { currentUser.require() } returns userId
        every { articleRepository.findBySlug(slug) } returns article
        every { commentRepository.findById(commentId) } returns comment

        assertThrows<ForbiddenException> {
            commentCommands.deleteComment(slug.value, 5L)
        }
    }
}
