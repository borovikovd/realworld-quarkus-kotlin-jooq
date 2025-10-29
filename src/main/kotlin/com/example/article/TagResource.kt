package com.example.article

import com.example.api.TagsApi
import com.example.api.model.GetTags200Response
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response

@Path("/api")
@ApplicationScoped
class TagResource : TagsApi {
    @Inject
    lateinit var articleService: ArticleService

    override fun getTags(): Response {
        val tags = articleService.getAllTags()

        return Response
            .ok(GetTags200Response().tags(tags))
            .build()
    }
}
