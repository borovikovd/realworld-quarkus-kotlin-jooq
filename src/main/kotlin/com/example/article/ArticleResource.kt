package com.example.article

import com.example.api.ArticlesApi
import com.example.api.model.CreateArticle201Response
import com.example.api.model.CreateArticleRequest
import com.example.api.model.GetArticlesFeed200Response
import com.example.api.model.UpdateArticleRequest
import com.example.shared.security.SecurityContext
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response

@ApplicationScoped
class ArticleResource(
    private val articleWriteService: ArticleWriteService,
    private val articleReadService: ArticleReadService,
    private val securityContext: SecurityContext,
) : ArticlesApi {
    @RolesAllowed("user")
    override fun createArticle(article: CreateArticleRequest): Response {
        val newArticle = article.article

        val articleId =
            articleWriteService.createArticle(
                title = newArticle.title,
                description = newArticle.description,
                body = newArticle.body,
                tags = newArticle.tagList ?: emptyList(),
            )

        val viewerId = securityContext.currentUserId
        val articleDto = articleReadService.hydrate(articleId, viewerId)

        return Response
            .status(Response.Status.CREATED)
            .entity(CreateArticle201Response().article(articleDto))
            .build()
    }

    @RolesAllowed("user")
    override fun deleteArticle(slug: String): Response {
        articleWriteService.deleteArticle(slug)

        return Response.noContent().build()
    }

    override fun getArticle(slug: String): Response {
        val viewerId = securityContext.currentUserId
        val articleDto = articleReadService.getArticleBySlug(slug, viewerId)

        return Response
            .ok(CreateArticle201Response().article(articleDto))
            .build()
    }

    @Suppress("UNCHECKED_CAST")
    override fun getArticles(
        tag: String?,
        author: String?,
        favorited: String?,
        offset: Int?,
        limit: Int?,
    ): Response {
        val viewerId = securityContext.currentUserId
        val articles =
            articleReadService.getArticles(
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
                    .articlesCount(articleReadService.countArticles(tag, author, favorited)),
            ).build()
    }

    @Suppress("UNCHECKED_CAST")
    @RolesAllowed("user")
    override fun getArticlesFeed(
        offset: Int?,
        limit: Int?,
    ): Response {
        val viewerId = securityContext.currentUserId!!
        val articles =
            articleReadService.getArticlesFeed(
                limit = limit ?: 20,
                offset = offset ?: 0,
                viewerId = viewerId,
            )

        return Response
            .ok(
                GetArticlesFeed200Response()
                    .articles(articles as List<com.example.api.model.GetArticlesFeed200ResponseArticlesInner>)
                    .articlesCount(articleReadService.countArticlesFeed(viewerId)),
            ).build()
    }

    @RolesAllowed("user")
    override fun updateArticle(
        slug: String,
        article: UpdateArticleRequest,
    ): Response {
        val updateData = article.article

        val articleId =
            articleWriteService.updateArticle(
                slug = slug,
                title = updateData.title,
                description = updateData.description,
                body = updateData.body,
            )

        val viewerId = securityContext.currentUserId
        val articleDto = articleReadService.hydrate(articleId, viewerId)

        return Response
            .ok(CreateArticle201Response().article(articleDto))
            .build()
    }
}
