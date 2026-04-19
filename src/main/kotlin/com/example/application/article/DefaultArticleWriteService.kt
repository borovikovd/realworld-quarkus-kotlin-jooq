package com.example.application.article

import com.example.domain.article.Article
import com.example.domain.article.ArticleRepository
import com.example.domain.article.SlugGenerator
import com.example.domain.shared.ForbiddenException
import com.example.domain.shared.NotFoundException
import com.example.domain.shared.ValidationException
import com.example.shared.architecture.WriteService
import com.example.shared.security.SecurityContext
import io.micrometer.core.annotation.Counted
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory

@WriteService
class DefaultArticleWriteService(
    private val articleRepository: ArticleRepository,
    private val slugGenerator: SlugGenerator,
    private val securityContext: SecurityContext,
) : ArticleWriteService {
    companion object {
        private val logger = LoggerFactory.getLogger(DefaultArticleWriteService::class.java)
    }

    @Counted("article.creation.count")
    @Transactional
    override fun createArticle(
        title: String,
        description: String,
        body: String,
        tags: List<String>,
    ): Long {
        validateArticleFields(title, description, body)

        val userId = securityContext.requireCurrentUserId()
        val articleId = articleRepository.nextId()
        val slug =
            slugGenerator.generateUniqueSlug(
                title = title,
                existingSlugChecker = { candidateSlug: String ->
                    articleRepository.findBySlug(candidateSlug) != null
                },
            )
        val article =
            Article(
                id = articleId,
                slug = slug,
                title = title,
                description = description,
                body = body,
                authorId = userId,
                tags = tags.toSet(),
            )
        articleRepository.create(article)
        return articleId.value
    }

    @Transactional
    override fun updateArticle(
        slug: String,
        title: String?,
        description: String?,
        body: String?,
    ): Long {
        val userId = securityContext.requireCurrentUserId()
        val article =
            articleRepository.findBySlug(slug)
                ?: throw NotFoundException("Article not found")

        if (userId != article.authorId) {
            throw ForbiddenException("You can only update your own articles")
        }

        validateArticleFields(title, description, body)

        val updatedTitle = title ?: article.title
        val updatedDescription = description ?: article.description
        val updatedBody = body ?: article.body

        val updatedSlug =
            if (title != null && title != article.title) {
                slugGenerator.generateUniqueSlug(
                    title = title,
                    existingSlugChecker = { candidateSlug: String ->
                        val existing = articleRepository.findBySlug(candidateSlug)
                        existing != null && existing.id != article.id
                    },
                )
            } else {
                article.slug
            }

        val updatedArticle = article.update(updatedSlug, updatedTitle, updatedDescription, updatedBody)
        articleRepository.update(updatedArticle)
        return article.id.value
    }

    @Transactional
    override fun deleteArticle(slug: String) {
        val userId = securityContext.requireCurrentUserId()
        val article =
            articleRepository.findBySlug(slug)
                ?: throw NotFoundException("Article not found")

        if (!article.canBeDeletedBy(userId)) {
            throw ForbiddenException("You can only delete your own articles")
        }

        articleRepository.deleteById(article.id)
        logger.info("Article deleted: articleId={}, slug={}", article.id.value, slug)
    }

    @Transactional
    override fun favoriteArticle(slug: String) {
        val userId = securityContext.requireCurrentUserId()
        val article =
            articleRepository.findBySlug(slug)
                ?: throw NotFoundException("Article not found")

        articleRepository.favorite(article.id, userId)
    }

    @Transactional
    override fun unfavoriteArticle(slug: String) {
        val userId = securityContext.requireCurrentUserId()
        val article =
            articleRepository.findBySlug(slug)
                ?: throw NotFoundException("Article not found")

        articleRepository.unfavorite(article.id, userId)
    }

    override fun getAllTags(): List<String> = articleRepository.getAllTags()

    private fun validateArticleFields(
        title: String?,
        description: String?,
        body: String?,
    ) {
        val errors = mutableMapOf<String, List<String>>()
        title?.let { if (it.isBlank()) errors["title"] = listOf("must not be blank") }
        description?.let { if (it.isBlank()) errors["description"] = listOf("must not be blank") }
        body?.let { if (it.isBlank()) errors["body"] = listOf("must not be blank") }
        if (errors.isNotEmpty()) throw ValidationException(errors)
    }
}
