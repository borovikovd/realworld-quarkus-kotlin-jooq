package com.example.application.usecase

import com.example.domain.aggregate.article.ArticleId

interface ArticleCommands {
    fun createArticle(
        title: String,
        description: String,
        body: String,
        tags: List<String>,
    ): ArticleId

    fun updateArticle(
        slug: String,
        title: String?,
        description: String?,
        body: String?,
    ): ArticleId

    fun deleteArticle(slug: String)

    fun favoriteArticle(slug: String)

    fun unfavoriteArticle(slug: String)
}
