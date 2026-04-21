package com.example.presentation.rest.article

import com.example.api.FavoritesApi
import com.example.api.model.CreateArticle201Response
import com.example.api.model.Profile
import com.example.application.ArticleService
import com.example.application.CurrentUser
import com.example.domain.article.readmodel.ArticleView
import com.example.domain.article.readmodel.ArticleViewReader
import com.example.domain.profile.readmodel.ProfileView
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import com.example.api.model.Article as ApiArticle

@ApplicationScoped
class FavoriteResource(
    private val articleService: ArticleService,
    private val articleViewReader: ArticleViewReader,
    private val currentUser: CurrentUser,
) : FavoritesApi {
    @RolesAllowed("user")
    override fun createArticleFavorite(slug: String): CreateArticle201Response {
        articleService.favoriteArticle(slug)

        val viewerId = currentUser.id?.value
        val articleDto = articleViewReader.getArticleBySlug(slug, viewerId).toDto()

        return CreateArticle201Response().article(articleDto)
    }

    @RolesAllowed("user")
    override fun deleteArticleFavorite(slug: String): CreateArticle201Response {
        articleService.unfavoriteArticle(slug)

        val viewerId = currentUser.id?.value
        val articleDto = articleViewReader.getArticleBySlug(slug, viewerId).toDto()

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
