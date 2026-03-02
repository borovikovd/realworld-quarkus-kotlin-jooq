package com.example.comment

import com.example.api.CommentsApi
import com.example.api.model.CreateArticleComment200Response
import com.example.api.model.CreateArticleCommentRequest
import com.example.api.model.GetArticleComments200Response
import com.example.api.model.Profile
import com.example.profile.ProfileSummary
import com.example.shared.security.SecurityContext
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.Response
import com.example.api.model.Comment as ApiComment

@ApplicationScoped
class CommentResource(
    private val commentWriteService: CommentWriteService,
    private val commentReadService: CommentReadService,
    private val securityContext: SecurityContext,
) : CommentsApi {
    @RolesAllowed("user")
    override fun createArticleComment(
        slug: String,
        comment: CreateArticleCommentRequest,
    ): Response {
        val viewerId = securityContext.currentUserId
        val newComment = comment.comment
        val commentId = commentWriteService.addComment(slug, newComment.body)

        val commentDto = commentReadService.hydrate(commentId, viewerId).toDto()

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
        commentWriteService.deleteComment(slug, id.toLong())

        return Response.noContent().build()
    }

    override fun getArticleComments(slug: String): Response {
        val viewerId = securityContext.currentUserId
        val comments = commentReadService.getCommentsBySlug(slug, viewerId).map { it.toDto() }

        return Response
            .ok(GetArticleComments200Response().comments(comments))
            .build()
    }
}

private fun CommentSummary.toDto(): ApiComment =
    ApiComment()
        .id(id.toInt())
        .body(body)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .author(author.toDto())

private fun ProfileSummary.toDto(): Profile =
    Profile()
        .username(username)
        .bio(bio)
        .image(image)
        .following(following)
