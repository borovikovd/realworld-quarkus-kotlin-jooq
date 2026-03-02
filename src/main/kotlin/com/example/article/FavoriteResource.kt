package com.example.article

import com.example.api.FavoritesApi
import com.example.api.model.CreateArticle201Response
import com.example.shared.security.SecurityContext
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response

@ApplicationScoped
class FavoriteResource(
    private val articleWriteService: ArticleWriteService,
    private val articleReadService: ArticleReadService,
    private val securityContext: SecurityContext,
) : FavoritesApi {
    @RolesAllowed("user")
    override fun createArticleFavorite(slug: String): Response {
        articleWriteService.favoriteArticle(slug)

        val viewerId = securityContext.currentUserId
        val articleDto = articleReadService.getArticleBySlug(slug, viewerId)

        return Response
            .ok(CreateArticle201Response().article(articleDto))
            .build()
    }

    @RolesAllowed("user")
    override fun deleteArticleFavorite(slug: String): Response {
        articleWriteService.unfavoriteArticle(slug)

        val viewerId = securityContext.currentUserId
        val articleDto = articleReadService.getArticleBySlug(slug, viewerId)

        return Response
            .ok(CreateArticle201Response().article(articleDto))
            .build()
    }
}
