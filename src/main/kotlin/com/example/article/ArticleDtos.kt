package com.example.article

import com.example.common.web.ListStringPatchDeserializer
import com.example.common.web.Patch
import com.example.user.ProfileDto
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

data class ArticleDto(
    val slug: String,
    val title: String,
    val description: String,
    val body: String,
    val tagList: List<String>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val favorited: Boolean,
    val favoritesCount: Int,
    val author: ProfileDto,
)

data class ArticleListItemDto(
    val slug: String,
    val title: String,
    val description: String,
    val tagList: List<String>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val favorited: Boolean,
    val favoritesCount: Int,
    val author: ProfileDto,
)

fun ArticleDto.toListItem() =
    ArticleListItemDto(
        slug = slug,
        title = title,
        description = description,
        tagList = tagList,
        createdAt = createdAt,
        updatedAt = updatedAt,
        favorited = favorited,
        favoritesCount = favoritesCount,
        author = author,
    )

data class ArticleEnvelope(
    val article: ArticleDto,
)

data class ArticleListEnvelope(
    val articles: List<ArticleListItemDto>,
    val articlesCount: Int,
)

data class NewArticleRequest(
    @field:Valid val article: NewArticle,
)

data class NewArticle(
    @field:NotBlank @field:Size(max = 256) val title: String,
    @field:NotBlank @field:Size(max = 1024) val description: String,
    @field:NotBlank val body: String,
    val tagList: List<String>? = null,
)

data class UpdateArticleRequest(
    @field:Valid val article: ArticlePatch,
)

data class ArticlePatch(
    @field:Size(min = 1, max = 256) val title: String?,
    @field:Size(min = 1, max = 1024) val description: String?,
    @field:Size(min = 1) val body: String?,
    @field:JsonDeserialize(using = ListStringPatchDeserializer::class) val tagList: Patch<List<String>> = Patch.Absent,
)
