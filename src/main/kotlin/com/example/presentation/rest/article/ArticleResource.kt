package com.example.presentation.rest.article

import com.example.api.ArticlesApi
import com.example.api.model.CreateArticle201Response
import com.example.api.model.CreateArticleRequest
import com.example.api.model.GetArticlesFeed200Response
import com.example.api.model.Profile
import com.example.api.model.UpdateArticleRequest
import com.example.application.command.ArticleCommands
import com.example.application.port.inbound.command.CreateArticleCommand
import com.example.application.port.inbound.command.DeleteArticleCommand
import com.example.application.port.inbound.command.UpdateArticleCommand
import com.example.application.port.inbound.query.CountArticlesFeedQuery
import com.example.application.port.inbound.query.CountArticlesQuery
import com.example.application.port.inbound.query.GetArticleByIdQuery
import com.example.application.port.inbound.query.GetArticleBySlugQuery
import com.example.application.port.inbound.query.GetArticlesFeedQuery
import com.example.application.port.inbound.query.ListArticlesQuery
import com.example.application.port.outbound.ArticleReadModel
import com.example.application.port.outbound.CurrentUser
import com.example.application.port.outbound.ProfileReadModel
import com.example.application.query.ArticleQueries
import com.example.domain.exception.NotFoundException
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.resteasy.reactive.ResponseStatus
import com.example.api.model.Article as ApiArticle

@ApplicationScoped
class ArticleResource(
    private val articleCommands: ArticleCommands,
    private val articleQueries: ArticleQueries,
    private val currentUser: CurrentUser,
) : ArticlesApi {
    @ResponseStatus(201)
    @RolesAllowed("user")
    override fun createArticle(article: CreateArticleRequest): CreateArticle201Response {
        val newArticle = article.article

        val articleId =
            articleCommands.createArticle(
                CreateArticleCommand(
                    title = newArticle.title,
                    description = newArticle.description,
                    body = newArticle.body,
                    tags = newArticle.tagList ?: emptyList(),
                ),
            )

        val viewerId = currentUser.id?.value
        val articleDto =
            (
                articleQueries.getArticleById(GetArticleByIdQuery(articleId, viewerId))
                    ?: throw NotFoundException("Article not found")
            ).toDto()

        return CreateArticle201Response().article(articleDto)
    }

    @ResponseStatus(204)
    @RolesAllowed("user")
    override fun deleteArticle(slug: String) {
        articleCommands.deleteArticle(DeleteArticleCommand(slug))
    }

    override fun getArticle(slug: String): CreateArticle201Response {
        val viewerId = currentUser.id?.value
        val articleDto =
            (
                articleQueries.getArticleBySlug(GetArticleBySlugQuery(slug, viewerId))
                    ?: throw NotFoundException("Article not found")
            ).toDto()

        return CreateArticle201Response().article(articleDto)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getArticles(
        tag: String?,
        author: String?,
        favorited: String?,
        offset: Int?,
        limit: Int?,
    ): GetArticlesFeed200Response {
        val viewerId = currentUser.id?.value
        val articles =
            articleQueries
                .getArticles(
                    ListArticlesQuery(
                        tag = tag,
                        author = author,
                        favorited = favorited,
                        limit = limit ?: 20,
                        offset = offset ?: 0,
                        viewerId = viewerId,
                    ),
                ).map { it.toDto() }

        return GetArticlesFeed200Response()
            .articles(articles as List<com.example.api.model.GetArticlesFeed200ResponseArticlesInner>)
            .articlesCount(articleQueries.countArticles(CountArticlesQuery(tag, author, favorited)))
    }

    @Suppress("UNCHECKED_CAST")
    @RolesAllowed("user")
    override fun getArticlesFeed(
        offset: Int?,
        limit: Int?,
    ): GetArticlesFeed200Response {
        val viewerId = currentUser.require().value
        val articles =
            articleQueries
                .getArticlesFeed(
                    GetArticlesFeedQuery(
                        viewerId = viewerId,
                        limit = limit ?: 20,
                        offset = offset ?: 0,
                    ),
                ).map { it.toDto() }

        return GetArticlesFeed200Response()
            .articles(articles as List<com.example.api.model.GetArticlesFeed200ResponseArticlesInner>)
            .articlesCount(articleQueries.countArticlesFeed(CountArticlesFeedQuery(viewerId)))
    }

    @RolesAllowed("user")
    override fun updateArticle(
        slug: String,
        article: UpdateArticleRequest,
    ): CreateArticle201Response {
        val updateData = article.article

        val articleId =
            articleCommands.updateArticle(
                UpdateArticleCommand(
                    slug = slug,
                    title = updateData.title,
                    description = updateData.description,
                    body = updateData.body,
                ),
            )

        val viewerId = currentUser.id?.value
        val articleDto =
            (
                articleQueries.getArticleById(GetArticleByIdQuery(articleId, viewerId))
                    ?: throw NotFoundException("Article not found")
            ).toDto()

        return CreateArticle201Response().article(articleDto)
    }
}

private fun ArticleReadModel.toDto(): ApiArticle =
    ApiArticle()
        .slug(slug)
        .title(title)
        .description(description)
        .body(body)
        .tagList(tagList)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .favorited(favorited)
        .favoritesCount(favoritesCount)
        .author(author.toDto())

private fun ProfileReadModel.toDto(): Profile =
    Profile()
        .username(username)
        .bio(bio)
        .image(image)
        .following(following)
