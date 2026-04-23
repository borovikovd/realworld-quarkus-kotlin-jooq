package com.example.application.query

import com.example.application.port.inbound.query.CountArticlesFeedQuery
import com.example.application.port.inbound.query.CountArticlesQuery
import com.example.application.port.inbound.query.GetAllTagsQuery
import com.example.application.port.inbound.query.GetArticleByIdQuery
import com.example.application.port.inbound.query.GetArticleBySlugQuery
import com.example.application.port.inbound.query.GetArticlesFeedQuery
import com.example.application.port.inbound.query.ListArticlesQuery
import com.example.application.port.outbound.ArticleReadModel

interface ArticleQueries {
    fun getArticleById(query: GetArticleByIdQuery): ArticleReadModel?

    fun getArticleBySlug(query: GetArticleBySlugQuery): ArticleReadModel?

    fun getArticles(query: ListArticlesQuery): List<ArticleReadModel>

    fun getArticlesFeed(query: GetArticlesFeedQuery): List<ArticleReadModel>

    fun countArticles(query: CountArticlesQuery): Int

    fun countArticlesFeed(query: CountArticlesFeedQuery): Int

    fun getAllTags(query: GetAllTagsQuery): List<String>
}
