package com.example.application.port

import com.example.application.port.Repository
import com.example.application.readmodel.ArticleReadModel
import com.example.domain.aggregate.article.Article
import com.example.domain.aggregate.article.ArticleId
import com.example.domain.aggregate.article.Slug
import com.example.domain.aggregate.user.UserId

interface ArticleRepository : Repository<Article, ArticleId> {
    fun findBySlug(slug: Slug): Article?

    fun deleteById(id: ArticleId)

    fun favorite(
        articleId: ArticleId,
        userId: UserId,
    )

    fun unfavorite(
        articleId: ArticleId,
        userId: UserId,
    )

    fun isFavorited(
        articleId: ArticleId,
        userId: UserId,
    ): Boolean

    fun findById(
        id: ArticleId,
        viewerId: UserId?,
    ): ArticleReadModel?

    fun findBySlug(
        slug: String,
        viewerId: UserId?,
    ): ArticleReadModel?

    fun list(
        tag: String?,
        author: String?,
        favorited: String?,
        limit: Int,
        offset: Int,
        viewerId: UserId?,
    ): List<ArticleReadModel>

    fun listFeed(
        viewerId: UserId,
        limit: Int,
        offset: Int,
    ): List<ArticleReadModel>

    fun count(
        tag: String?,
        author: String?,
        favorited: String?,
    ): Int

    fun countFeed(viewerId: UserId): Int

    fun allTags(): List<String>
}
