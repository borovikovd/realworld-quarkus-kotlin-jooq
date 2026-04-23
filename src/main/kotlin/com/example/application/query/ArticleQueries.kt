package com.example.application.query

import com.example.application.port.outbound.ArticleReadRepository
import com.example.application.query.readmodel.ArticleReadModel
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class ArticleQueries(
    private val articleReadRepository: ArticleReadRepository,
) {
    fun getArticleById(
        id: Long,
        viewerId: Long?,
    ): ArticleReadModel? = articleReadRepository.findById(id, viewerId)

    fun getArticleBySlug(
        slug: String,
        viewerId: Long?,
    ): ArticleReadModel? = articleReadRepository.findBySlug(slug, viewerId)

    fun getArticles(
        tag: String?,
        author: String?,
        favorited: String?,
        limit: Int,
        offset: Int,
        viewerId: Long?,
    ): List<ArticleReadModel> = articleReadRepository.list(tag, author, favorited, limit, offset, viewerId)

    fun getArticlesFeed(
        viewerId: Long,
        limit: Int,
        offset: Int,
    ): List<ArticleReadModel> = articleReadRepository.listFeed(viewerId, limit, offset)

    fun countArticles(
        tag: String?,
        author: String?,
        favorited: String?,
    ): Int = articleReadRepository.count(tag, author, favorited)

    fun countArticlesFeed(viewerId: Long): Int = articleReadRepository.countFeed(viewerId)

    fun getAllTags(): List<String> = articleReadRepository.allTags()
}
