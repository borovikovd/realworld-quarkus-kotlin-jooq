package com.example.comment.infrastructure

import com.example.api.CommentsApi
import com.example.api.model.CreateArticleComment200Response
import com.example.api.model.CreateArticleCommentRequest
import com.example.api.model.GetArticleComments200Response
import com.example.api.model.Profile
import com.example.comment.application.CommentReadService
import com.example.comment.application.CommentSummary
import com.example.comment.application.CommentWriteService
import com.example.profile.application.ProfileSummary
import com.example.shared.security.SecurityContext
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.resteasy.reactive.ResponseStatus
import com.example.api.model.Comment as ApiComment

@ApplicationScoped
class CommentResource(
    private val commentWriteService: CommentWriteService,
    private val commentReadService: CommentReadService,
    private val securityContext: SecurityContext,
) : CommentsApi {
    @ResponseStatus(201)
    @RolesAllowed("user")
    override fun createArticleComment(
        slug: String,
        comment: CreateArticleCommentRequest,
    ): CreateArticleComment200Response {
        val viewerId = securityContext.currentUserId?.value
        val newComment = comment.comment
        val commentId = commentWriteService.addComment(slug, newComment.body)

        val commentDto = commentReadService.hydrate(commentId, viewerId).toDto()

        return CreateArticleComment200Response().comment(commentDto)
    }

    @ResponseStatus(204)
    @RolesAllowed("user")
    override fun deleteArticleComment(
        slug: String,
        id: Int,
    ) {
        commentWriteService.deleteComment(slug, id.toLong())
    }

    override fun getArticleComments(slug: String): GetArticleComments200Response {
        val viewerId = securityContext.currentUserId?.value
        val comments = commentReadService.getCommentsBySlug(slug, viewerId).map { it.toDto() }

        return GetArticleComments200Response().comments(comments)
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
