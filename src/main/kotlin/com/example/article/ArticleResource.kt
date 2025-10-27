package com.example.article

import com.example.api.ArticlesApi
import com.example.api.FavoritesApi
import com.example.api.model.CreateArticle201Response
import com.example.api.model.CreateArticleRequest
import com.example.api.model.GetArticlesFeed200Response
import com.example.api.model.UpdateArticleRequest
import com.example.shared.security.SecurityContext
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response

@ApplicationScoped
class ArticleResource :
    ArticlesApi,
    FavoritesApi {
    @Inject
    lateinit var articleService: ArticleService

    @Inject
    lateinit var articleQueries: ArticleQueries

    @Inject
    lateinit var securityContext: SecurityContext

    @RolesAllowed("**")
    override fun createArticle(article: CreateArticleRequest): Response {
        val userId = securityContext.currentUserId!!
        val newArticle = article.article

        val created =
            articleService.createArticle(
                userId = userId,
                title = newArticle.title,
                description = newArticle.description,
                body = newArticle.body,
                tags = newArticle.tagList ?: emptyList(),
            )

        val articleDto = articleQueries.getArticleBySlug(created.slug, userId)

        return Response
            .status(Response.Status.CREATED)
            .entity(CreateArticle201Response().article(articleDto))
            .build()
    }

    @RolesAllowed("**")
    override fun deleteArticle(slug: String): Response {
        val userId = securityContext.currentUserId!!
        articleService.deleteArticle(userId, slug)

        return Response.ok().build()
    }

    override fun getArticle(slug: String): Response {
        val viewerId = securityContext.currentUserId
        val articleDto = articleQueries.getArticleBySlug(slug, viewerId)

        return Response
            .ok(CreateArticle201Response().article(articleDto))
            .build()
    }

    override fun getArticles(
        tag: String?,
        author: String?,
        favorited: String?,
        offset: Int?,
        limit: Int?,
    ): Response {
        val viewerId = securityContext.currentUserId
        val articles =
            articleQueries.getArticles(
                tag = tag,
                author = author,
                favorited = favorited,
                limit = limit ?: 20,
                offset = offset ?: 0,
                viewerId = viewerId,
            )

        return Response
            .ok(
                GetArticlesFeed200Response()
                    .articles(articles as List<com.example.api.model.GetArticlesFeed200ResponseArticlesInner>)
                    .articlesCount(articles.size),
            ).build()
    }

    @RolesAllowed("**")
    override fun getArticlesFeed(
        offset: Int?,
        limit: Int?,
    ): Response {
        val viewerId = securityContext.currentUserId!!
        val articles =
            articleQueries.getArticlesFeed(
                limit = limit ?: 20,
                offset = offset ?: 0,
                viewerId = viewerId,
            )

        return Response
            .ok(
                GetArticlesFeed200Response()
                    .articles(articles as List<com.example.api.model.GetArticlesFeed200ResponseArticlesInner>)
                    .articlesCount(articles.size),
            ).build()
    }

    @RolesAllowed("**")
    override fun updateArticle(
        slug: String,
        article: UpdateArticleRequest,
    ): Response {
        val userId = securityContext.currentUserId!!
        val updateData = article.article

        val updated =
            articleService.updateArticle(
                userId = userId,
                slug = slug,
                title = updateData.title,
                description = updateData.description,
                body = updateData.body,
            )

        val articleDto = articleQueries.getArticleBySlug(updated.slug, userId)

        return Response
            .ok(CreateArticle201Response().article(articleDto))
            .build()
    }

    @RolesAllowed("**")
    override fun createArticleFavorite(slug: String): Response {
        val userId = securityContext.currentUserId!!
        articleService.favoriteArticle(userId, slug)

        val articleDto = articleQueries.getArticleBySlug(slug, userId)

        return Response
            .ok(CreateArticle201Response().article(articleDto))
            .build()
    }

    @RolesAllowed("**")
    override fun deleteArticleFavorite(slug: String): Response {
        val userId = securityContext.currentUserId!!
        articleService.unfavoriteArticle(userId, slug)

        val articleDto = articleQueries.getArticleBySlug(slug, userId)

        return Response
            .ok(CreateArticle201Response().article(articleDto))
            .build()
    }
}
