package com.example.application.article

import com.example.domain.article.Article
import com.example.domain.article.ArticleId
import com.example.domain.article.ArticleRepository
import com.example.domain.shared.ForbiddenException
import com.example.domain.shared.NotFoundException
import com.example.domain.shared.ValidationException
import com.example.shared.security.SecurityContext
import com.example.domain.article.SlugGenerator
import com.example.domain.user.UserId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultArticleWriteServiceTest {
    private lateinit var articleWriteService: DefaultArticleWriteService
    private lateinit var articleRepository: ArticleRepository
    private lateinit var slugGenerator: SlugGenerator
    private lateinit var securityContext: SecurityContext

    @BeforeEach
    fun setup() {
        articleRepository = mockk()
        slugGenerator = mockk()
        securityContext = mockk()
        articleWriteService = DefaultArticleWriteService(
            articleRepository = articleRepository,
            slugGenerator = slugGenerator,
            securityContext = securityContext,
        )
    }

    @Test
    fun `createArticle should throw ValidationException when title is blank`() {
        val exception =
            assertThrows<ValidationException> {
                articleWriteService.createArticle("", "Test description", "Test body", emptyList())
            }

        assertEquals(listOf("must not be blank"), exception.errors["title"])
    }

    @Test
    fun `createArticle should throw ValidationException with multiple blank fields`() {
        val exception =
            assertThrows<ValidationException> {
                articleWriteService.createArticle("", "", "", emptyList())
            }

        assertEquals(3, exception.errors.size)
        assertTrue(exception.errors.containsKey("title"))
        assertTrue(exception.errors.containsKey("description"))
        assertTrue(exception.errors.containsKey("body"))
    }

    @Test
    fun `updateArticle should throw ValidationException when title is blank`() {
        val userId = UserId(1L)
        val slug = "test-slug"

        every { securityContext.requireCurrentUserId() } returns userId

        val existingArticle =
            Article(
                id = ArticleId(1L),
                slug = slug,
                title = "Original Title",
                description = "Original description",
                body = "Original body",
                authorId = userId,
            )

        every { articleRepository.findBySlug(slug) } returns existingArticle

        val exception =
            assertThrows<ValidationException> {
                articleWriteService.updateArticle(slug, " ", null, null)
            }

        assertEquals(listOf("must not be blank"), exception.errors["title"])
    }

    @Test
    fun `createArticle should generate slug and return article id`() {
        val userId = UserId(1L)
        val articleId = ArticleId(10L)
        val title = "Test Article"
        val description = "Test description"
        val body = "Test body"
        val tags = listOf("tag1", "tag2")
        val generatedSlug = "test-article"

        every { securityContext.requireCurrentUserId() } returns userId
        every { articleRepository.nextId() } returns articleId

        every {
            slugGenerator.generateUniqueSlug(
                title = title,
                existingSlugChecker = any(),
            )
        } returns generatedSlug

        every { articleRepository.create(any()) } answers { firstArg() }

        val result = articleWriteService.createArticle(title, description, body, tags)

        assertEquals(articleId.value, result)
        verify { articleRepository.nextId() }
        verify { slugGenerator.generateUniqueSlug(title = title, existingSlugChecker = any()) }
        verify { articleRepository.create(any()) }
    }

    @Test
    fun `updateArticle should update all fields and return article id`() {
        val userId = UserId(1L)
        val originalSlug = "original-slug"
        val newTitle = "New Title"
        val newDescription = "New description"
        val newBody = "New body"
        val newSlug = "new-title"

        every { securityContext.requireCurrentUserId() } returns userId

        val existingArticle =
            Article(
                id = ArticleId(1L),
                slug = originalSlug,
                title = "Original Title",
                description = "Original description",
                body = "Original body",
                authorId = userId,
            )

        every { articleRepository.findBySlug(originalSlug) } returns existingArticle
        every {
            slugGenerator.generateUniqueSlug(
                title = newTitle,
                existingSlugChecker = any(),
            )
        } returns newSlug

        every { articleRepository.update(any()) } answers { firstArg() }

        val result = articleWriteService.updateArticle(originalSlug, newTitle, newDescription, newBody)

        assertEquals(1L, result)
        verify { articleRepository.findBySlug(originalSlug) }
        verify { slugGenerator.generateUniqueSlug(title = newTitle, existingSlugChecker = any()) }
        verify { articleRepository.update(any()) }
    }

    @Test
    fun `updateArticle should keep existing values when null provided`() {
        val userId = UserId(1L)
        val slug = "test-slug"

        every { securityContext.requireCurrentUserId() } returns userId

        val existingArticle =
            Article(
                id = ArticleId(1L),
                slug = slug,
                title = "Original Title",
                description = "Original description",
                body = "Original body",
                authorId = userId,
            )

        every { articleRepository.findBySlug(slug) } returns existingArticle
        every { articleRepository.update(any()) } answers { firstArg() }

        val result = articleWriteService.updateArticle(slug, null, null, null)

        assertEquals(1L, result)
        verify { articleRepository.findBySlug(slug) }
        verify(exactly = 0) { slugGenerator.generateUniqueSlug(any(), any()) }
        verify { articleRepository.update(any()) }
    }

    @Test
    fun `updateArticle should throw NotFoundException when article not found`() {
        val userId = UserId(1L)
        val slug = "non-existent"

        every { securityContext.requireCurrentUserId() } returns userId
        every { articleRepository.findBySlug(slug) } returns null

        assertThrows<NotFoundException> {
            articleWriteService.updateArticle(slug, "New Title", null, null)
        }
    }

    @Test
    fun `updateArticle should throw ForbiddenException when user is not author`() {
        val userId = UserId(1L)
        val differentUserId = UserId(2L)
        val slug = "test-slug"

        every { securityContext.requireCurrentUserId() } returns userId

        val existingArticle =
            Article(
                id = ArticleId(1L),
                slug = slug,
                title = "Test Article",
                description = "Test description",
                body = "Test body",
                authorId = differentUserId,
            )

        every { articleRepository.findBySlug(slug) } returns existingArticle

        assertThrows<ForbiddenException> {
            articleWriteService.updateArticle(slug, "New Title", null, null)
        }
    }

    @Test
    fun `deleteArticle should delete when user is author`() {
        val userId = UserId(1L)
        val slug = "test-slug"

        every { securityContext.requireCurrentUserId() } returns userId

        val article =
            Article(
                id = ArticleId(1L),
                slug = slug,
                title = "Test Article",
                description = "Test description",
                body = "Test body",
                authorId = userId,
            )

        every { articleRepository.findBySlug(slug) } returns article
        every { articleRepository.deleteById(ArticleId(1L)) } returns Unit

        articleWriteService.deleteArticle(slug)

        verify { articleRepository.findBySlug(slug) }
        verify { articleRepository.deleteById(ArticleId(1L)) }
    }

    @Test
    fun `deleteArticle should throw NotFoundException when article not found`() {
        val userId = UserId(1L)
        val slug = "non-existent"

        every { securityContext.requireCurrentUserId() } returns userId
        every { articleRepository.findBySlug(slug) } returns null

        assertThrows<NotFoundException> {
            articleWriteService.deleteArticle(slug)
        }
    }

    @Test
    fun `deleteArticle should throw ForbiddenException when user is not author`() {
        val userId = UserId(1L)
        val differentUserId = UserId(2L)
        val slug = "test-slug"

        every { securityContext.requireCurrentUserId() } returns userId

        val article =
            Article(
                id = ArticleId(1L),
                slug = slug,
                title = "Test Article",
                description = "Test description",
                body = "Test body",
                authorId = differentUserId,
            )

        every { articleRepository.findBySlug(slug) } returns article

        assertThrows<ForbiddenException> {
            articleWriteService.deleteArticle(slug)
        }
    }

    @Test
    fun `favoriteArticle should call repository favorite`() {
        val userId = UserId(1L)
        val slug = "test-slug"

        every { securityContext.requireCurrentUserId() } returns userId

        val article =
            Article(
                id = ArticleId(1L),
                slug = slug,
                title = "Test Article",
                description = "Test description",
                body = "Test body",
                authorId = UserId(2L),
            )

        every { articleRepository.findBySlug(slug) } returns article
        every { articleRepository.favorite(ArticleId(1L), userId) } returns Unit

        articleWriteService.favoriteArticle(slug)

        verify { articleRepository.findBySlug(slug) }
        verify { articleRepository.favorite(ArticleId(1L), userId) }
    }

    @Test
    fun `favoriteArticle should throw NotFoundException when article not found`() {
        val userId = UserId(1L)
        val slug = "non-existent"

        every { securityContext.requireCurrentUserId() } returns userId
        every { articleRepository.findBySlug(slug) } returns null

        assertThrows<NotFoundException> {
            articleWriteService.favoriteArticle(slug)
        }
    }

    @Test
    fun `unfavoriteArticle should call repository unfavorite`() {
        val userId = UserId(1L)
        val slug = "test-slug"

        every { securityContext.requireCurrentUserId() } returns userId

        val article =
            Article(
                id = ArticleId(1L),
                slug = slug,
                title = "Test Article",
                description = "Test description",
                body = "Test body",
                authorId = UserId(2L),
            )

        every { articleRepository.findBySlug(slug) } returns article
        every { articleRepository.unfavorite(ArticleId(1L), userId) } returns Unit

        articleWriteService.unfavoriteArticle(slug)

        verify { articleRepository.findBySlug(slug) }
        verify { articleRepository.unfavorite(ArticleId(1L), userId) }
    }

    @Test
    fun `getAllTags should return tags from repository`() {
        val tags = listOf("tag1", "tag2", "tag3")

        every { articleRepository.getAllTags() } returns tags

        val result = articleWriteService.getAllTags()

        assertEquals(tags, result)
        verify { articleRepository.getAllTags() }
    }
}
