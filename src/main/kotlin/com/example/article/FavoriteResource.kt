package com.example.article

import com.example.api.FavoritesApi
import com.example.api.model.CreateArticle201Response
import com.example.shared.security.SecurityContext
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response

@Path("/api")
@ApplicationScoped
class FavoriteResource(
    private val articleService: ArticleService,
    private val articleDataService: ArticleDataService,
    private val securityContext: SecurityContext,
) : FavoritesApi {
    @RolesAllowed("user")
    override fun createArticleFavorite(slug: String): Response {
        articleService.favoriteArticle(slug)

        val viewerId = securityContext.currentUserId
        val articleDto = articleDataService.getArticleBySlug(slug, viewerId)

        return Response
            .ok(CreateArticle201Response().article(articleDto))
            .build()
    }

    @RolesAllowed("user")
    override fun deleteArticleFavorite(slug: String): Response {
        articleService.unfavoriteArticle(slug)

        val viewerId = securityContext.currentUserId
        val articleDto = articleDataService.getArticleBySlug(slug, viewerId)

        return Response
            .ok(CreateArticle201Response().article(articleDto))
            .build()
    }
}
