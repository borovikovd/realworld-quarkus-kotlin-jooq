package com.example.article

import com.example.common.security.CurrentUser
import com.example.common.web.ForbiddenException
import com.example.common.web.NotFoundException
import com.example.common.web.Patch
import com.example.common.web.ValidationException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

@ApplicationScoped
class ArticleService(
    private val articleRepository: ArticleRepository,
    private val currentUser: CurrentUser,
) {
    @Transactional
    fun create(
        title: String,
        description: String,
        body: String,
        tags: List<String>,
    ): ArticleDto {
        val userId = currentUser.require()
        val articleId = articleRepository.nextId()
        val slug = SlugGenerator.generateUniqueSlug(title) { candidate -> articleRepository.existsBySlug(candidate) }
        val now = OffsetDateTime.now()

        articleRepository.insert(
            Article(
                id = articleId,
                slug = slug,
                title = title,
                description = description,
                body = body,
                authorId = userId,
                tags = tags.toSet(),
                createdAt = now,
                updatedAt = now,
            ),
        )

        return articleRepository.findDtoById(articleId, userId)
            ?: error("Article not found after insert: id=${articleId.value}")
    }

    @Transactional
    fun update(
        slug: String,
        title: String?,
        description: String?,
        body: String?,
        tagList: Patch<List<String>>,
    ): ArticleDto {
        val userId = currentUser.require()
        val article = articleRepository.findBySlug(slug) ?: throw NotFoundException("article", "Article not found")

        if (userId != article.authorId) throw ForbiddenException("article", "You can only update your own articles")

        val newSlug =
            if (title != null && title != article.title) {
                SlugGenerator.generateUniqueSlug(title) { candidate ->
                    val existing = articleRepository.findBySlug(candidate)
                    existing != null && existing.id != article.id
                }
            } else {
                article.slug
            }

        val tags =
            when (tagList) {
                Patch.Absent -> article.tags
                Patch.Null -> throw ValidationException(mapOf("tagList" to listOf("must not be null")))
                is Patch.Value -> tagList.value.toSet()
            }

        val updated =
            article.copy(
                slug = newSlug,
                title = title ?: article.title,
                description = description ?: article.description,
                body = body ?: article.body,
                tags = tags,
                updatedAt = OffsetDateTime.now(),
            )

        articleRepository.update(updated)

        return articleRepository.findDtoById(article.id, userId)
            ?: error("Article not found after update: id=${article.id.value}")
    }

    @Transactional
    fun delete(slug: String) {
        val userId = currentUser.require()
        val article = articleRepository.findBySlug(slug) ?: throw NotFoundException("article", "Article not found")
        if (userId != article.authorId) throw ForbiddenException("article", "You can only delete your own articles")
        articleRepository.deleteById(article.id)
        logger.info("Article deleted: articleId={}, slug={}", article.id.value, slug)
    }

    @Transactional
    fun favorite(slug: String): ArticleDto {
        val userId = currentUser.require()
        val article = articleRepository.findBySlug(slug) ?: throw NotFoundException("article", "Article not found")
        articleRepository.favorite(article.id, userId)
        return articleRepository.findDtoBySlug(slug, userId)
            ?: error("Article not found after favorite: slug=$slug")
    }

    @Transactional
    fun unfavorite(slug: String): ArticleDto {
        val userId = currentUser.require()
        val article = articleRepository.findBySlug(slug) ?: throw NotFoundException("article", "Article not found")
        articleRepository.unfavorite(article.id, userId)
        return articleRepository.findDtoBySlug(slug, userId)
            ?: error("Article not found after unfavorite: slug=$slug")
    }

    fun getBySlug(slug: String): ArticleDto? = articleRepository.findDtoBySlug(slug, currentUser.id)

    fun list(
        filter: ArticleFilter,
        page: Page,
    ): List<ArticleListItemDto> = articleRepository.list(filter, page, currentUser.id)

    fun count(filter: ArticleFilter): Int = articleRepository.count(filter)

    fun feed(page: Page): List<ArticleListItemDto> = articleRepository.feed(currentUser.require(), page)

    fun feedCount(): Int = articleRepository.feedCount(currentUser.require())

    companion object {
        private val logger = LoggerFactory.getLogger(ArticleService::class.java)
    }
}
