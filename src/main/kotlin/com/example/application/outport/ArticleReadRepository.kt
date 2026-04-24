package com.example.application.outport

import com.example.application.readmodel.ArticleReadModel

interface ArticleReadRepository {
    fun findById(
        id: Long,
        viewerId: Long?,
    ): ArticleReadModel?

    fun findBySlug(
        slug: String,
        viewerId: Long?,
    ): ArticleReadModel?

    fun list(
        tag: String?,
        author: String?,
        favorited: String?,
        limit: Int,
        offset: Int,
        viewerId: Long?,
    ): List<ArticleReadModel>

    fun listFeed(
        viewerId: Long,
        limit: Int,
        offset: Int,
    ): List<ArticleReadModel>

    fun count(
        tag: String?,
        author: String?,
        favorited: String?,
    ): Int

    fun countFeed(viewerId: Long): Int

    fun allTags(): List<String>
}
