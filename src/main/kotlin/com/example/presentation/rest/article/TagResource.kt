package com.example.presentation.rest.article

import com.example.api.TagsApi
import com.example.api.model.GetTags200Response
import com.example.application.port.outbound.ArticleReadRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class TagResource(
    private val articleReadRepository: ArticleReadRepository,
) : TagsApi {
    override fun getTags(): GetTags200Response = GetTags200Response().tags(articleReadRepository.getAllTags())
}
