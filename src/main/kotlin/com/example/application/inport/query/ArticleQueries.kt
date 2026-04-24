package com.example.application.inport.query

import com.example.application.readmodel.ArticleReadModel

interface ArticleQueries {
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
        viewerId: Long,
        limit: Int,
        offset: Int,
    ): List<ArticleReadModel>

    fun countArticles(
        tag: String?,
        author: String?,
        favorited: String?,
    ): Int

    fun countArticlesFeed(viewerId: Long): Int

    fun getAllTags(): List<String>
}
