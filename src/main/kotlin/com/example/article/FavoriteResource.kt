package com.example.article

import com.example.api.FavoritesApi
import com.example.api.model.CreateArticle201Response
import com.example.shared.security.SecurityContext
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response

@ApplicationScoped
class FavoriteResource : FavoritesApi {
    @Inject
    lateinit var articleService: ArticleService

    @Inject
    lateinit var articleQueries: ArticleQueries

    @Inject
    lateinit var securityContext: SecurityContext

    @RolesAllowed("**")
    override fun createArticleFavorite(slug: String): Response {
        articleService.favoriteArticle(slug)

        val viewerId = securityContext.currentUserId
        val articleDto = articleQueries.getArticleBySlug(slug, viewerId)

        return Response
            .ok(CreateArticle201Response().article(articleDto))
            .build()
    }

    @RolesAllowed("**")
    override fun deleteArticleFavorite(slug: String): Response {
        articleService.unfavoriteArticle(slug)

        val viewerId = securityContext.currentUserId
        val articleDto = articleQueries.getArticleBySlug(slug, viewerId)

        return Response
            .ok(CreateArticle201Response().article(articleDto))
            .build()
    }
}
