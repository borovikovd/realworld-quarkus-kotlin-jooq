package com.example.presentation.rest.comment

import com.example.api.CommentsApi
import com.example.api.model.CreateArticleComment200Response
import com.example.api.model.CreateArticleCommentRequest
import com.example.api.model.GetArticleComments200Response
import com.example.api.model.Profile
import com.example.application.CurrentUser
import com.example.application.comment.CommentWriteService
import com.example.domain.comment.CommentViewReader
import com.example.domain.comment.readmodel.CommentView
import com.example.domain.profile.readmodel.ProfileView
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.resteasy.reactive.ResponseStatus
import com.example.api.model.Comment as ApiComment

@ApplicationScoped
class CommentResource(
    private val commentWriteService: CommentWriteService,
    private val commentViewReader: CommentViewReader,
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
        val commentId = commentWriteService.addComment(slug, newComment.body)

        val commentDto = commentViewReader.hydrate(commentId, viewerId).toDto()

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
        val viewerId = currentUser.id?.value
        val comments = commentViewReader.getCommentsBySlug(slug, viewerId).map { it.toDto() }

        return GetArticleComments200Response().comments(comments)
    }
}

private fun CommentView.toDto(): ApiComment =
    ApiComment()
        .id(id.toInt())
        .body(body)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .author(author.toDto())

private fun ProfileView.toDto(): Profile =
    Profile()
        .username(username)
        .bio(bio)
        .image(image)
        .following(following)
