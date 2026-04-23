package com.example.application.command

import com.example.application.port.inbound.command.CreateArticleCommand
import com.example.application.port.inbound.command.DeleteArticleCommand
import com.example.application.port.inbound.command.FavoriteArticleCommand
import com.example.application.port.inbound.command.UnfavoriteArticleCommand
import com.example.application.port.inbound.command.UpdateArticleCommand
import com.example.application.port.outbound.ArticleWriteRepository
import com.example.application.port.outbound.Clock
import com.example.application.port.outbound.CurrentUser
import com.example.domain.aggregate.article.Article
import com.example.domain.aggregate.article.Slug
import com.example.domain.exception.ForbiddenException
import com.example.domain.exception.NotFoundException
import com.example.domain.exception.ValidationException
import com.example.domain.service.SlugGenerator
import io.micrometer.core.annotation.Counted
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory

@ApplicationScoped
class ArticleCommands(
    private val articleWriteRepository: ArticleWriteRepository,
    private val currentUser: CurrentUser,
    private val clock: Clock,
) {
    @Counted("article.creation.count")
    @Transactional
    fun createArticle(command: CreateArticleCommand): Long {
        validateArticleFields(command.title, command.description, command.body)

        val userId = currentUser.require()
        val articleId = articleWriteRepository.nextId()
        val slug =
            SlugGenerator.generateUniqueSlug(
                title = command.title,
                existingSlugChecker = { candidate -> articleWriteRepository.findBySlug(candidate) != null },
            )
        val article =
            Article(
                id = articleId,
                slug = slug,
                title = command.title,
                description = command.description,
                body = command.body,
                authorId = userId,
                tags = command.tags.toSet(),
            )
        articleWriteRepository.create(article)
        return articleId.value
    }

    @Transactional
    fun updateArticle(command: UpdateArticleCommand): Long {
        val userId = currentUser.require()
        val article =
            articleWriteRepository.findBySlug(Slug(command.slug))
                ?: throw NotFoundException("Article not found")

        if (userId != article.authorId) {
            throw ForbiddenException("You can only update your own articles")
        }

        validateArticleFields(command.title, command.description, command.body)

        val updatedTitle = command.title ?: article.title
        val updatedDescription = command.description ?: article.description
        val updatedBody = command.body ?: article.body

        val newTitle = command.title
        val updatedSlug =
            if (newTitle != null && newTitle != article.title) {
                SlugGenerator.generateUniqueSlug(
                    title = newTitle,
                    existingSlugChecker = { candidate ->
                        val existing = articleWriteRepository.findBySlug(candidate)
                        existing != null && existing.id != article.id
                    },
                )
            } else {
                article.slug
            }

        val updatedArticle = article.update(updatedSlug, updatedTitle, updatedDescription, updatedBody, clock.now())
        articleWriteRepository.update(updatedArticle)
        return article.id.value
    }

    @Transactional
    fun deleteArticle(command: DeleteArticleCommand) {
        val userId = currentUser.require()
        val article =
            articleWriteRepository.findBySlug(Slug(command.slug))
                ?: throw NotFoundException("Article not found")

        if (!article.canBeDeletedBy(userId)) {
            throw ForbiddenException("You can only delete your own articles")
        }

        articleWriteRepository.deleteById(article.id)
        logger.info("Article deleted: articleId={}, slug={}", article.id.value, command.slug)
    }

    @Transactional
    fun favoriteArticle(command: FavoriteArticleCommand) {
        val userId = currentUser.require()
        val article =
            articleWriteRepository.findBySlug(Slug(command.slug))
                ?: throw NotFoundException("Article not found")

        articleWriteRepository.favorite(article.id, userId)
    }

    @Transactional
    fun unfavoriteArticle(command: UnfavoriteArticleCommand) {
        val userId = currentUser.require()
        val article =
            articleWriteRepository.findBySlug(Slug(command.slug))
                ?: throw NotFoundException("Article not found")

        articleWriteRepository.unfavorite(article.id, userId)
    }

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
        private val logger = LoggerFactory.getLogger(ArticleCommands::class.java)
    }
}
