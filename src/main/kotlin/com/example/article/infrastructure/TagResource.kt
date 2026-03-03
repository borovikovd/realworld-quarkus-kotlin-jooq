package com.example.article.infrastructure

import com.example.api.TagsApi
import com.example.api.model.GetTags200Response
import com.example.article.application.ArticleWriteService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response

@ApplicationScoped
class TagResource(
    private val articleWriteService: ArticleWriteService,
) : TagsApi {
    override fun getTags(): Response {
        val tags = articleWriteService.getAllTags()

        return Response
            .ok(GetTags200Response().tags(tags))
            .build()
    }
}
