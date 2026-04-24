package com.example.application.inport.command

import com.example.application.inport.command.CommentCommands
import com.example.application.outport.ArticleWriteRepository
import com.example.application.outport.CommentReadRepository
import com.example.application.outport.CommentWriteRepository
import com.example.application.outport.CurrentUser
import com.example.application.service.CommentApplicationService
import com.example.domain.aggregate.article.Article
import com.example.domain.aggregate.article.ArticleId
import com.example.domain.aggregate.article.Body
import com.example.domain.aggregate.article.Description
import com.example.domain.aggregate.article.Slug
import com.example.domain.aggregate.article.Title
import com.example.domain.aggregate.comment.Comment
import com.example.domain.aggregate.comment.CommentId
import com.example.domain.aggregate.user.UserId
import com.example.domain.exception.ForbiddenException
import com.example.domain.exception.NotFoundException
import com.example.domain.exception.ValidationException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class CommentCommandsTest {
    private lateinit var commentCommands: CommentCommands
    private lateinit var commentWriteRepository: CommentWriteRepository
    private lateinit var articleWriteRepository: ArticleWriteRepository
    private lateinit var currentUser: CurrentUser

    private val userId = UserId(1L)
    private val articleId = ArticleId(10L)
    private val slug = Slug("test-article")
    private val article = Article(
        id = articleId,
        slug = slug,
        title = Title("Test Article"),
        description = Description("desc"),
        body = Body("body"),
        authorId = UserId(2L),
    )

    @BeforeEach
    fun setup() {
        commentWriteRepository = mockk()
        articleWriteRepository = mockk()
        currentUser = mockk()
        commentCommands = CommentApplicationService(
            commentWriteRepository = commentWriteRepository,
            commentReadRepository = mockk(relaxed = true),
            articleWriteRepository = articleWriteRepository,
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
        every { articleWriteRepository.findBySlug(slug) } returns article
        every { commentWriteRepository.nextId() } returns commentId
        every { commentWriteRepository.create(any()) } answers { firstArg() }

        val result = commentCommands.addComment(slug.value, body)

        assertEquals(commentId.value, result)
        verify { commentWriteRepository.nextId() }
        verify { commentWriteRepository.create(match { it.articleId == articleId && it.authorId == userId && it.body == body }) }
    }

    @Test
    fun `addComment should throw NotFoundException when article not found`() {
        every { currentUser.require() } returns userId
        every { articleWriteRepository.findBySlug(slug) } returns null

        assertThrows<NotFoundException> {
            commentCommands.addComment(slug.value, "comment body")
        }
    }

    @Test
    fun `deleteComment should delete when user is author`() {
        val commentId = CommentId(5L)
        val comment = Comment(id = commentId, articleId = articleId, authorId = userId, body = "My comment")

        every { currentUser.require() } returns userId
        every { articleWriteRepository.findBySlug(slug) } returns article
        every { commentWriteRepository.findById(commentId) } returns comment
        every { commentWriteRepository.deleteById(commentId) } returns Unit

        commentCommands.deleteComment(slug.value, 5L)

        verify { commentWriteRepository.deleteById(commentId) }
    }

    @Test
    fun `deleteComment should throw NotFoundException when article not found`() {
        every { currentUser.require() } returns userId
        every { articleWriteRepository.findBySlug(slug) } returns null

        assertThrows<NotFoundException> {
            commentCommands.deleteComment(slug.value, 5L)
        }
    }

    @Test
    fun `deleteComment should throw NotFoundException when comment not found`() {
        every { currentUser.require() } returns userId
        every { articleWriteRepository.findBySlug(slug) } returns article
        every { commentWriteRepository.findById(CommentId(5L)) } returns null

        assertThrows<NotFoundException> {
            commentCommands.deleteComment(slug.value, 5L)
        }
    }

    @Test
    fun `deleteComment should throw NotFoundException when comment belongs to different article`() {
        val commentId = CommentId(5L)
        val comment = Comment(id = commentId, articleId = ArticleId(999L), authorId = userId, body = "Wrong article")

        every { currentUser.require() } returns userId
        every { articleWriteRepository.findBySlug(slug) } returns article
        every { commentWriteRepository.findById(commentId) } returns comment

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
        every { articleWriteRepository.findBySlug(slug) } returns article
        every { commentWriteRepository.findById(commentId) } returns comment

        assertThrows<ForbiddenException> {
            commentCommands.deleteComment(slug.value, 5L)
        }
    }
}
