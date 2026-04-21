package com.example.presentation.rest.article

import com.example.api.TagsApi
import com.example.api.model.GetTags200Response
import com.example.application.article.ArticleService
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class TagResource(
    private val articleService: ArticleService,
) : TagsApi {
    override fun getTags(): GetTags200Response {
        val tags = articleService.getAllTags()

        return GetTags200Response().tags(tags)
    }
}
