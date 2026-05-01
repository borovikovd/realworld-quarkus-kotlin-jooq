package com.example.application.usecase.article

interface ArticleCommands {
    fun createArticle(
        title: String,
        description: String,
        body: String,
        tags: List<String>,
    ): Long

    fun updateArticle(
        slug: String,
        title: String?,
        description: String?,
        body: String?,
    ): Long

    fun deleteArticle(slug: String)

    fun favoriteArticle(slug: String)

    fun unfavoriteArticle(slug: String)
}
