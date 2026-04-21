package com.example.presentation.rest.article

import com.example.api.ArticlesApi
import com.example.api.model.CreateArticle201Response
import com.example.api.model.CreateArticleRequest
import com.example.api.model.GetArticlesFeed200Response
import com.example.api.model.Profile
import com.example.api.model.UpdateArticleRequest
import com.example.application.CurrentUser
import com.example.application.article.ArticleWriteService
import com.example.domain.article.ArticleViewReader
import com.example.domain.article.readmodel.ArticleView
import com.example.domain.profile.readmodel.ProfileView
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.resteasy.reactive.ResponseStatus
import com.example.api.model.Article as ApiArticle

@ApplicationScoped
class ArticleResource(
    private val articleWriteService: ArticleWriteService,
    private val articleViewReader: ArticleViewReader,
    private val currentUser: CurrentUser,
) : ArticlesApi {
    @ResponseStatus(201)
    @RolesAllowed("user")
    override fun createArticle(article: CreateArticleRequest): CreateArticle201Response {
        val newArticle = article.article

        val articleId =
            articleWriteService.createArticle(
                title = newArticle.title,
                description = newArticle.description,
                body = newArticle.body,
                tags = newArticle.tagList ?: emptyList(),
            )

        val viewerId = currentUser.id?.value
        val articleDto = articleViewReader.hydrate(articleId, viewerId).toDto()

        return CreateArticle201Response().article(articleDto)
    }

    @ResponseStatus(204)
    @RolesAllowed("user")
    override fun deleteArticle(slug: String) {
        articleWriteService.deleteArticle(slug)
    }

    override fun getArticle(slug: String): CreateArticle201Response {
        val viewerId = currentUser.id?.value
        val articleDto = articleViewReader.getArticleBySlug(slug, viewerId).toDto()

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
            articleViewReader
                .getArticles(
                    tag = tag,
                    author = author,
                    favorited = favorited,
                    limit = limit ?: 20,
                    offset = offset ?: 0,
                    viewerId = viewerId,
                ).map { it.toDto() }

        return GetArticlesFeed200Response()
            .articles(articles as List<com.example.api.model.GetArticlesFeed200ResponseArticlesInner>)
            .articlesCount(articleViewReader.countArticles(tag, author, favorited))
    }

    @Suppress("UNCHECKED_CAST")
    @RolesAllowed("user")
    override fun getArticlesFeed(
        offset: Int?,
        limit: Int?,
    ): GetArticlesFeed200Response {
        val viewerId = currentUser.require().value
        val articles =
            articleViewReader
                .getArticlesFeed(
                    limit = limit ?: 20,
                    offset = offset ?: 0,
                    viewerId = viewerId,
                ).map { it.toDto() }

        return GetArticlesFeed200Response()
            .articles(articles as List<com.example.api.model.GetArticlesFeed200ResponseArticlesInner>)
            .articlesCount(articleViewReader.countArticlesFeed(viewerId))
    }

    @RolesAllowed("user")
    override fun updateArticle(
        slug: String,
        article: UpdateArticleRequest,
    ): CreateArticle201Response {
        val updateData = article.article

        val articleId =
            articleWriteService.updateArticle(
                slug = slug,
                title = updateData.title,
                description = updateData.description,
                body = updateData.body,
            )

        val viewerId = currentUser.id?.value
        val articleDto = articleViewReader.hydrate(articleId, viewerId).toDto()

        return CreateArticle201Response().article(articleDto)
    }
}

private fun ArticleView.toDto(): ApiArticle =
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

private fun ProfileView.toDto(): Profile =
    Profile()
        .username(username)
        .bio(bio)
        .image(image)
        .following(following)
