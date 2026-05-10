package com.example.comment

import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.jboss.resteasy.reactive.ResponseStatus

@ApplicationScoped
@Path("/articles/{slug}/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class CommentResource(
    private val commentService: CommentService,
) {
    @POST
    @RolesAllowed("user")
    @ResponseStatus(201)
    @APIResponse(responseCode = "201", description = "Created")
    @APIResponse(responseCode = "404", description = "Not Found")
    fun addComment(
        @PathParam("slug") slug: String,
        @Valid body: NewCommentRequest,
    ): CommentEnvelope = CommentEnvelope(commentService.add(slug, body.comment.body))

    @GET
    @APIResponse(responseCode = "404", description = "Not Found")
    fun getComments(
        @PathParam("slug") slug: String,
    ): CommentListEnvelope = CommentListEnvelope(commentService.listByArticle(slug))

    @DELETE
    @Path("/{id}")
    @RolesAllowed("user")
    @ResponseStatus(204)
    @APIResponse(responseCode = "404", description = "Not Found")
    fun deleteComment(
        @PathParam("slug") slug: String,
        @PathParam("id") id: Long,
    ) {
        commentService.delete(slug, CommentId(id))
    }
}
