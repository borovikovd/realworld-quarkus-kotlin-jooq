package com.example.application.service

import com.example.application.inport.command.ArticleCommands
import com.example.application.inport.query.ArticleQueries
import com.example.application.outport.ArticleRepository
import com.example.application.outport.Clock
import com.example.application.outport.CurrentUser
import com.example.application.readmodel.ArticleReadModel
import com.example.domain.aggregate.article.Article
import com.example.domain.aggregate.article.Body
import com.example.domain.aggregate.article.Description
import com.example.domain.aggregate.article.Slug
import com.example.domain.aggregate.article.Tag
import com.example.domain.aggregate.article.Title
import com.example.domain.exception.ForbiddenException
import com.example.domain.exception.NotFoundException
import com.example.domain.exception.ValidationException
import com.example.domain.service.SlugGenerator
import io.micrometer.core.annotation.Counted
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory

@ApplicationScoped
class ArticleApplicationService(
    private val articleRepository: ArticleRepository,
    private val currentUser: CurrentUser,
    private val clock: Clock,
) : ArticleCommands,
    ArticleQueries {
    @Counted("article.creation.count")
    @Transactional
    override fun createArticle(
        title: String,
        description: String,
        body: String,
        tags: List<String>,
    ): Long {
        validateArticleFields(title, description, body)

        val userId = currentUser.require()
        val articleId = articleRepository.nextId()
        val slug =
            SlugGenerator.generateUniqueSlug(
                title = title,
                existingSlugChecker = { candidate -> articleRepository.findBySlug(candidate) != null },
            )
        val article =
            Article(
                id = articleId,
                slug = slug,
                title = Title(title),
                description = Description(description),
                body = Body(body),
                authorId = userId,
                tags = tags.map { Tag(it) }.toSet(),
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
        val userId = currentUser.require()
        val article =
            articleRepository.findBySlug(Slug(slug))
                ?: throw NotFoundException("Article not found")

        if (userId != article.authorId) {
            throw ForbiddenException("You can only update your own articles")
        }

        validateArticleFields(title, description, body)

        val updatedTitle = title?.let { Title(it) } ?: article.title
        val updatedDescription = description?.let { Description(it) } ?: article.description
        val updatedBody = body?.let { Body(it) } ?: article.body

        val updatedSlug =
            if (title != null && title != article.title.value) {
                SlugGenerator.generateUniqueSlug(
                    title = title,
                    existingSlugChecker = { candidate ->
                        val existing = articleRepository.findBySlug(candidate)
                        existing != null && existing.id != article.id
                    },
                )
            } else {
                article.slug
            }

        val updatedArticle = article.update(updatedSlug, updatedTitle, updatedDescription, updatedBody, clock.now())
        articleRepository.update(updatedArticle)
        return article.id.value
    }

    @Transactional
    override fun deleteArticle(slug: String) {
        val userId = currentUser.require()
        val article =
            articleRepository.findBySlug(Slug(slug))
                ?: throw NotFoundException("Article not found")

        if (!article.canBeDeletedBy(userId)) {
            throw ForbiddenException("You can only delete your own articles")
        }

        articleRepository.deleteById(article.id)
        logger.info("Article deleted: articleId={}, slug={}", article.id.value, slug)
    }

    @Transactional
    override fun favoriteArticle(slug: String) {
        val userId = currentUser.require()
        val article =
            articleRepository.findBySlug(Slug(slug))
                ?: throw NotFoundException("Article not found")

        articleRepository.favorite(article.id, userId)
    }

    @Transactional
    override fun unfavoriteArticle(slug: String) {
        val userId = currentUser.require()
        val article =
            articleRepository.findBySlug(Slug(slug))
                ?: throw NotFoundException("Article not found")

        articleRepository.unfavorite(article.id, userId)
    }

    override fun getArticleById(
        id: Long,
        viewerId: Long?,
    ): ArticleReadModel? = articleRepository.findById(id, viewerId)

    override fun getArticleBySlug(
        slug: String,
        viewerId: Long?,
    ): ArticleReadModel? = articleRepository.findBySlug(slug, viewerId)

    override fun getArticles(
        tag: String?,
        author: String?,
        favorited: String?,
        limit: Int,
        offset: Int,
        viewerId: Long?,
    ): List<ArticleReadModel> = articleRepository.list(tag, author, favorited, limit, offset, viewerId)

    override fun getArticlesFeed(
        viewerId: Long,
        limit: Int,
        offset: Int,
    ): List<ArticleReadModel> = articleRepository.listFeed(viewerId, limit, offset)

    override fun countArticles(
        tag: String?,
        author: String?,
        favorited: String?,
    ): Int = articleRepository.count(tag, author, favorited)

    override fun countArticlesFeed(viewerId: Long): Int = articleRepository.countFeed(viewerId)

    override fun getAllTags(): List<String> = articleRepository.allTags()

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

    companion object {
        private val logger = LoggerFactory.getLogger(ArticleApplicationService::class.java)
    }
}
