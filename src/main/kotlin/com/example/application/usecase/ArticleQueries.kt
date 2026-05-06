package com.example.application.usecase

import com.example.application.readmodel.ArticleReadModel
import com.example.domain.aggregate.article.ArticleId
import com.example.domain.aggregate.user.UserId

interface ArticleQueries {
    fun getArticleById(
        id: ArticleId,
        viewerId: UserId?,
    ): ArticleReadModel?

    fun getArticleBySlug(
        slug: String,
        viewerId: UserId?,
    ): ArticleReadModel?

    fun getArticles(
        tag: String?,
        author: String?,
        favorited: String?,
        limit: Int,
        offset: Int,
        viewerId: UserId?,
    ): List<ArticleReadModel>

    fun getArticlesFeed(
        viewerId: UserId,
        limit: Int,
        offset: Int,
    ): List<ArticleReadModel>

    fun countArticles(
        tag: String?,
        author: String?,
        favorited: String?,
    ): Int

    fun countArticlesFeed(viewerId: UserId): Int

    fun getAllTags(): List<String>
}
