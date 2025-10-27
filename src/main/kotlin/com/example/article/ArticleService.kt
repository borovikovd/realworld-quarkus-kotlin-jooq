package com.example.article

import com.example.shared.exceptions.ForbiddenException
import com.example.shared.exceptions.NotFoundException
import com.example.shared.utils.SlugGenerator
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional

@ApplicationScoped
class ArticleService {
    @Inject
    lateinit var articleRepository: ArticleRepository

    @Inject
    lateinit var slugGenerator: SlugGenerator

    @Transactional
    fun createArticle(
        userId: Long,
        title: String,
        description: String,
        body: String,
        tags: List<String>,
    ): Article {
        val slug =
            slugGenerator.generateUniqueSlug(
                title = title,
                existingSlugChecker = { candidateSlug: String ->
                    articleRepository.findBySlug(candidateSlug) != null
                },
            )
        val article =
            Article(
                slug = slug,
                title = title,
                description = description,
                body = body,
                authorId = userId,
                tags = tags.toSet(),
            )
        return articleRepository.create(article)
    }

    @Transactional
    fun updateArticle(
        userId: Long,
        slug: String,
        title: String?,
        description: String?,
        body: String?,
    ): Article {
        val article =
            articleRepository.findBySlug(slug)
                ?: throw NotFoundException("Article not found")

        if (userId != article.authorId) {
            throw ForbiddenException("You can only update your own articles")
        }

        val updatedTitle = if (title != null && title.isNotBlank()) title else article.title
        val updatedDescription =
            if (description != null && description.isNotBlank()) description else article.description
        val updatedBody = if (body != null && body.isNotBlank()) body else article.body

        val updatedSlug =
            if (title != null && title.isNotBlank() && title != article.title) {
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
        return articleRepository.update(updatedArticle)
    }

    @Transactional
    fun deleteArticle(
        userId: Long,
        slug: String,
    ) {
        val article =
            articleRepository.findBySlug(slug)
                ?: throw NotFoundException("Article not found")

        if (!article.canBeDeletedBy(userId)) {
            throw ForbiddenException("You can only delete your own articles")
        }

        articleRepository.deleteById(article.id!!)
    }

    @Transactional
    fun favoriteArticle(
        userId: Long,
        slug: String,
    ) {
        val article =
            articleRepository.findBySlug(slug)
                ?: throw NotFoundException("Article not found")

        articleRepository.favorite(article.id!!, userId)
    }

    @Transactional
    fun unfavoriteArticle(
        userId: Long,
        slug: String,
    ) {
        val article =
            articleRepository.findBySlug(slug)
                ?: throw NotFoundException("Article not found")

        articleRepository.unfavorite(article.id!!, userId)
    }

    fun getArticle(slug: String): Article =
        articleRepository.findBySlug(slug)
            ?: throw NotFoundException("Article not found")

    fun getAllTags(): List<String> = articleRepository.getAllTags()
}
