package com.example.article.infrastructure

import com.example.api.TagsApi
import com.example.api.model.GetTags200Response
import com.example.application.article.ArticleWriteService
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class TagResource(
    private val articleWriteService: ArticleWriteService,
) : TagsApi {
    override fun getTags(): GetTags200Response {
        val tags = articleWriteService.getAllTags()

        return GetTags200Response().tags(tags)
    }
}
