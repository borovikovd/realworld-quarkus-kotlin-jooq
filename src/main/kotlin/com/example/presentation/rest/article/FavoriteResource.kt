package com.example.presentation.rest.article

import com.example.api.FavoritesApi
import com.example.api.model.CreateArticle201Response
import com.example.api.model.Profile
import com.example.application.CurrentUser
import com.example.application.article.ArticleReadService
import com.example.application.article.ArticleSummary
import com.example.application.article.ArticleWriteService
import com.example.application.profile.ProfileSummary
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import com.example.api.model.Article as ApiArticle

@ApplicationScoped
class FavoriteResource(
    private val articleWriteService: ArticleWriteService,
    private val articleReadService: ArticleReadService,
    private val currentUser: CurrentUser,
) : FavoritesApi {
    @RolesAllowed("user")
    override fun createArticleFavorite(slug: String): CreateArticle201Response {
        articleWriteService.favoriteArticle(slug)

        val viewerId = currentUser.id?.value
        val articleDto = articleReadService.getArticleBySlug(slug, viewerId).toDto()

        return CreateArticle201Response().article(articleDto)
    }

    @RolesAllowed("user")
    override fun deleteArticleFavorite(slug: String): CreateArticle201Response {
        articleWriteService.unfavoriteArticle(slug)

        val viewerId = currentUser.id?.value
        val articleDto = articleReadService.getArticleBySlug(slug, viewerId).toDto()

        return CreateArticle201Response().article(articleDto)
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
