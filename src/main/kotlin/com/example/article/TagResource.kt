package com.example.article

import com.example.article.ArticleService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@ApplicationScoped
@Path("/tags")
@Produces(MediaType.APPLICATION_JSON)
class TagResource(
    private val articleService: ArticleService,
) {
    @GET
    fun getTags(): Map<String, List<String>> = mapOf("tags" to articleService.allTags())
}
