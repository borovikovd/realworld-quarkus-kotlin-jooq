package com.example.article

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@ApplicationScoped
@Path("/tags")
@Produces(MediaType.APPLICATION_JSON)
class TagResource(
    private val articleRepository: ArticleRepository,
) {
    @GET
    fun getTags(): TagsResponse = TagsResponse(articleRepository.allTags())
}
