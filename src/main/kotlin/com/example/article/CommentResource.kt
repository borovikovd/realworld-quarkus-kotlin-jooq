package com.example.article

import com.example.api.CommentsApi
import com.example.api.model.CreateArticleComment200Response
import com.example.api.model.CreateArticleCommentRequest
import com.example.api.model.GetArticleComments200Response
import com.example.shared.security.SecurityContext
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response

@ApplicationScoped
class CommentResource : CommentsApi {
    @Inject
    lateinit var commentService: CommentService

    @Inject
    lateinit var commentQueries: CommentQueries

    @Inject
    lateinit var securityContext: SecurityContext

    @RolesAllowed("**")
    override fun createArticleComment(
        slug: String,
        comment: CreateArticleCommentRequest,
    ): Response {
        val userId = securityContext.currentUserId!!
        val newComment = comment.comment
        val createdComment = commentService.addComment(userId, slug, newComment.body)

        val commentDto = commentQueries.getCommentById(createdComment.id!!, userId)

        return Response
            .ok(CreateArticleComment200Response().comment(commentDto))
            .build()
    }

    @RolesAllowed("**")
    override fun deleteArticleComment(
        slug: String,
        id: Int,
    ): Response {
        val userId = securityContext.currentUserId!!
        commentService.deleteComment(userId, slug, id.toLong())

        return Response.ok().build()
    }

    override fun getArticleComments(slug: String): Response {
        val viewerId = securityContext.currentUserId
        val comments = commentQueries.getCommentsBySlug(slug, viewerId)

        return Response
            .ok(GetArticleComments200Response().comments(comments))
            .build()
    }
}
