package com.example.presentation.rest.article

import com.example.api.FavoritesApi
import com.example.api.model.CreateArticle201Response
import com.example.api.model.Profile
import com.example.application.command.ArticleCommands
import com.example.application.port.outbound.ArticleReadModel
import com.example.application.port.outbound.ArticleReadRepository
import com.example.application.port.outbound.CurrentUser
import com.example.application.port.outbound.ProfileReadModel
import com.example.domain.exception.NotFoundException
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import com.example.api.model.Article as ApiArticle

@ApplicationScoped
class FavoriteResource(
    private val articleCommands: ArticleCommands,
    private val articleReadRepository: ArticleReadRepository,
    private val currentUser: CurrentUser,
) : FavoritesApi {
    @RolesAllowed("user")
    override fun createArticleFavorite(slug: String): CreateArticle201Response {
        articleCommands.favoriteArticle(slug)

        val viewerId = currentUser.id?.value
        val articleDto =
            (articleReadRepository.getArticleBySlug(slug, viewerId) ?: throw NotFoundException("Article not found"))
                .toDto()

        return CreateArticle201Response().article(articleDto)
    }

    @RolesAllowed("user")
    override fun deleteArticleFavorite(slug: String): CreateArticle201Response {
        articleCommands.unfavoriteArticle(slug)

        val viewerId = currentUser.id?.value
        val articleDto =
            (articleReadRepository.getArticleBySlug(slug, viewerId) ?: throw NotFoundException("Article not found"))
                .toDto()

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
