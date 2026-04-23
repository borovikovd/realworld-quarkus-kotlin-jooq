package com.example.application.port.outbound

import com.example.application.port.outbound.ArticleReadModel

interface ArticleReadRepository {
    fun getArticleById(
        id: Long,
        viewerId: Long?,
    ): ArticleReadModel?

    fun getArticleBySlug(
        slug: String,
        viewerId: Long?,
    ): ArticleReadModel?

    fun getArticles(
        tag: String?,
        author: String?,
        favorited: String?,
        limit: Int,
        offset: Int,
        viewerId: Long?,
    ): List<ArticleReadModel>

    fun getArticlesFeed(
        limit: Int,
        offset: Int,
        viewerId: Long,
    ): List<ArticleReadModel>

    fun countArticles(
        tag: String?,
        author: String?,
        favorited: String?,
    ): Int

    fun countArticlesFeed(viewerId: Long): Int

    fun getAllTags(): List<String>
}
