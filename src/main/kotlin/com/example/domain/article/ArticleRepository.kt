package com.example.domain.article

import com.example.domain.shared.Repository
import com.example.domain.user.UserId

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

    fun getAllTags(): List<String>
}
