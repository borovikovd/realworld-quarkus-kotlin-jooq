package com.example.comment

import com.example.api.CommentsApi
import com.example.api.model.CreateArticleComment200Response
import com.example.api.model.CreateArticleCommentRequest
import com.example.api.model.GetArticleComments200Response
import com.example.shared.security.SecurityContext
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response

@ApplicationScoped
class CommentResource(
    private val commentService: CommentService,
    private val commentDataService: CommentDataService,
    private val securityContext: SecurityContext,
) : CommentsApi {
    @RolesAllowed("user")
    override fun createArticleComment(
        slug: String,
        comment: CreateArticleCommentRequest,
    ): Response {
        val viewerId = securityContext.currentUserId
        val newComment = comment.comment
        val commentId = commentService.addComment(slug, newComment.body)

        val commentDto = commentDataService.hydrate(commentId, viewerId)

        return Response
            .status(Response.Status.CREATED)
            .entity(CreateArticleComment200Response().comment(commentDto))
            .build()
    }

    @RolesAllowed("user")
    override fun deleteArticleComment(
        slug: String,
        id: Int,
    ): Response {
        commentService.deleteComment(slug, id.toLong())

        return Response.noContent().build()
    }

    override fun getArticleComments(slug: String): Response {
        val viewerId = securityContext.currentUserId
        val comments = commentDataService.getCommentsBySlug(slug, viewerId)

        return Response
            .ok(GetArticleComments200Response().comments(comments))
            .build()
    }
}
