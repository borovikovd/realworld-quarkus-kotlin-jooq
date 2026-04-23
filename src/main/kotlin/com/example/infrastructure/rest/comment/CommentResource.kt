package com.example.infrastructure.rest.comment

import com.example.api.CommentsApi
import com.example.api.model.CreateArticleComment200Response
import com.example.api.model.CreateArticleCommentRequest
import com.example.api.model.GetArticleComments200Response
import com.example.api.model.Profile
import com.example.application.command.CommentCommands
import com.example.application.port.inbound.command.AddCommentCommand
import com.example.application.port.inbound.command.DeleteCommentCommand
import com.example.application.port.inbound.query.GetCommentByIdQuery
import com.example.application.port.inbound.query.GetCommentsBySlugQuery
import com.example.application.port.outbound.CommentReadModel
import com.example.application.port.outbound.CurrentUser
import com.example.application.port.outbound.ProfileReadModel
import com.example.application.query.CommentQueries
import com.example.domain.exception.NotFoundException
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.resteasy.reactive.ResponseStatus
import com.example.api.model.Comment as ApiComment

@ApplicationScoped
class CommentResource(
    private val commentCommands: CommentCommands,
    private val commentQueries: CommentQueries,
    private val currentUser: CurrentUser,
) : CommentsApi {
    @ResponseStatus(201)
    @RolesAllowed("user")
    override fun createArticleComment(
        slug: String,
        comment: CreateArticleCommentRequest,
    ): CreateArticleComment200Response {
        val viewerId = currentUser.id?.value
        val newComment = comment.comment
        val commentId = commentCommands.addComment(AddCommentCommand(slug, newComment.body))

        val commentDto =
            (
                commentQueries.getCommentById(GetCommentByIdQuery(commentId, viewerId))
                    ?: throw NotFoundException("Comment not found")
            ).toDto()

        return CreateArticleComment200Response().comment(commentDto)
    }

    @ResponseStatus(204)
    @RolesAllowed("user")
    override fun deleteArticleComment(
        slug: String,
        id: Int,
    ) {
        commentCommands.deleteComment(DeleteCommentCommand(slug, id.toLong()))
    }

    override fun getArticleComments(slug: String): GetArticleComments200Response {
        val viewerId = currentUser.id?.value
        val comments = commentQueries.getCommentsBySlug(GetCommentsBySlugQuery(slug, viewerId)).map { it.toDto() }

        return GetArticleComments200Response().comments(comments)
    }
}

private fun CommentReadModel.toDto(): ApiComment =
    ApiComment()
        .id(id.toInt())
        .body(body)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .author(author.toDto())

private fun ProfileReadModel.toDto(): Profile =
    Profile()
        .username(username)
        .bio(bio)
        .image(image)
        .following(following)
