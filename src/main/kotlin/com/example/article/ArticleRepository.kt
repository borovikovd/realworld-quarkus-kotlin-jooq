package com.example.article

import com.example.shared.domain.Repository
import com.example.user.UserId

interface ArticleRepository : Repository<Article, ArticleId> {
    fun findBySlug(slug: String): Article?

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
