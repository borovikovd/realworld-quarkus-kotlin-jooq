package com.example.domain.article.readmodel

interface ArticleViewReader {
    fun getArticleById(
        id: Long,
        viewerId: Long?,
    ): ArticleView

    fun getArticleBySlug(
        slug: String,
        viewerId: Long?,
    ): ArticleView

    fun getArticles(
        tag: String?,
        author: String?,
        favorited: String?,
        limit: Int,
        offset: Int,
        viewerId: Long?,
    ): List<ArticleView>

    fun getArticlesFeed(
        limit: Int,
        offset: Int,
        viewerId: Long,
    ): List<ArticleView>

    fun countArticles(
        tag: String?,
        author: String?,
        favorited: String?,
    ): Int

    fun countArticlesFeed(viewerId: Long): Int
}
