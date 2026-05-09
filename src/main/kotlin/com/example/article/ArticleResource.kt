package com.example.article

import com.example.common.web.NotFoundException
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import org.jboss.resteasy.reactive.ResponseStatus

@ApplicationScoped
@Path("/articles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class ArticleResource(
    private val articleService: ArticleService,
) {
    @POST
    @RolesAllowed("user")
    @ResponseStatus(201)
    fun createArticle(
        @Valid body: NewArticleRequest,
    ): ArticleEnvelope {
        val a = body.article
        return ArticleEnvelope(articleService.create(a.title, a.description, a.body, a.tagList))
    }

    @GET
    fun getArticles(
        @QueryParam("tag") tag: String?,
        @QueryParam("author") author: String?,
        @QueryParam("favorited") favorited: String?,
        @QueryParam("limit") @DefaultValue("20") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ): ArticleListEnvelope {
        val filter = ArticleFilter(tag, author, favorited)
        val page = Page(limit, offset)
        return ArticleListEnvelope(
            articles = articleService.list(filter, page).map { it.toListItem() },
            articlesCount = articleService.count(filter),
        )
    }

    @GET
    @Path("/feed")
    @RolesAllowed("user")
    fun getArticlesFeed(
        @QueryParam("limit") @DefaultValue("20") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ): ArticleListEnvelope {
        val page = Page(limit, offset)
        return ArticleListEnvelope(
            articles = articleService.feed(page).map { it.toListItem() },
            articlesCount = articleService.feedCount(),
        )
    }

    @GET
    @Path("/{slug}")
    fun getArticle(
        @PathParam("slug") slug: String,
    ): ArticleEnvelope {
        val dto = articleService.getBySlug(slug) ?: throw NotFoundException("article", "Article not found")
        return ArticleEnvelope(dto)
    }

    @PUT
    @Path("/{slug}")
    @RolesAllowed("user")
    fun updateArticle(
        @PathParam("slug") slug: String,
        @Valid body: UpdateArticleRequest,
    ): ArticleEnvelope {
        val p = body.article
        return ArticleEnvelope(articleService.update(slug, p.title, p.description, p.body, p.tagList))
    }

    @DELETE
    @Path("/{slug}")
    @RolesAllowed("user")
    @ResponseStatus(204)
    fun deleteArticle(
        @PathParam("slug") slug: String,
    ) {
        articleService.delete(slug)
    }

    @POST
    @Path("/{slug}/favorite")
    @RolesAllowed("user")
    fun createArticleFavorite(
        @PathParam("slug") slug: String,
    ): ArticleEnvelope = ArticleEnvelope(articleService.favorite(slug))

    @DELETE
    @Path("/{slug}/favorite")
    @RolesAllowed("user")
    fun deleteArticleFavorite(
        @PathParam("slug") slug: String,
    ): ArticleEnvelope = ArticleEnvelope(articleService.unfavorite(slug))
}
