package com.example.article.application

interface ArticleReadService {
    fun hydrate(
        id: Long,
        viewerId: Long?,
    ): ArticleSummary

    fun getArticleBySlug(
        slug: String,
        viewerId: Long?,
    ): ArticleSummary

    fun getArticles(
        tag: String?,
        author: String?,
        favorited: String?,
        limit: Int,
        offset: Int,
        viewerId: Long?,
    ): List<ArticleSummary>

    fun getArticlesFeed(
        limit: Int,
        offset: Int,
        viewerId: Long,
    ): List<ArticleSummary>

    fun countArticles(
        tag: String?,
        author: String?,
        favorited: String?,
    ): Int

    fun countArticlesFeed(viewerId: Long): Int
}
