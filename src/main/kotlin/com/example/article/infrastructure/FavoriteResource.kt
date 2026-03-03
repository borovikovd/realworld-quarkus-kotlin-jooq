package com.example.article.infrastructure

import com.example.api.FavoritesApi
import com.example.api.model.CreateArticle201Response
import com.example.api.model.Profile
import com.example.article.application.ArticleReadService
import com.example.article.application.ArticleSummary
import com.example.article.application.ArticleWriteService
import com.example.profile.application.ProfileSummary
import com.example.shared.security.SecurityContext
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response
import com.example.api.model.Article as ApiArticle

@ApplicationScoped
class FavoriteResource(
    private val articleWriteService: ArticleWriteService,
    private val articleReadService: ArticleReadService,
    private val securityContext: SecurityContext,
) : FavoritesApi {
    @RolesAllowed("user")
    override fun createArticleFavorite(slug: String): Response {
        articleWriteService.favoriteArticle(slug)

        val viewerId = securityContext.currentUserId?.value
        val articleDto = articleReadService.getArticleBySlug(slug, viewerId).toDto()

        return Response
            .ok(CreateArticle201Response().article(articleDto))
            .build()
    }

    @RolesAllowed("user")
    override fun deleteArticleFavorite(slug: String): Response {
        articleWriteService.unfavoriteArticle(slug)

        val viewerId = securityContext.currentUserId?.value
        val articleDto = articleReadService.getArticleBySlug(slug, viewerId).toDto()

        return Response
            .ok(CreateArticle201Response().article(articleDto))
            .build()
    }
}

private fun ArticleSummary.toDto(): ApiArticle =
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

private fun ProfileSummary.toDto(): Profile =
    Profile()
        .username(username)
        .bio(bio)
        .image(image)
        .following(following)
