package com.example.application.port

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
}
