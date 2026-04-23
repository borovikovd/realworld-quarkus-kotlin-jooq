package com.example.application.command

import com.example.application.port.inbound.command.AddCommentCommand
import com.example.application.port.inbound.command.DeleteCommentCommand
import com.example.application.port.outbound.ArticleWriteRepository
import com.example.application.port.outbound.CommentWriteRepository
import com.example.application.port.outbound.CurrentUser
import com.example.domain.aggregate.article.Slug
import com.example.domain.aggregate.comment.Comment
import com.example.domain.aggregate.comment.CommentId
import com.example.domain.exception.ForbiddenException
import com.example.domain.exception.NotFoundException
import com.example.domain.exception.ValidationException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory

@ApplicationScoped
class CommentCommands(
    private val commentWriteRepository: CommentWriteRepository,
    private val articleWriteRepository: ArticleWriteRepository,
    private val currentUser: CurrentUser,
) {
    @Transactional
    fun addComment(command: AddCommentCommand): Long {
        if (command.body.isBlank()) {
            throw ValidationException(mapOf("body" to listOf("must not be blank")))
        }

        val userId = currentUser.require()
        val article =
            articleWriteRepository.findBySlug(Slug(command.articleSlug))
                ?: throw NotFoundException("Article not found")

        val commentId = commentWriteRepository.nextId()
        val comment =
            Comment(
                id = commentId,
                articleId = article.id,
                authorId = userId,
                body = command.body,
            )
        commentWriteRepository.create(comment)
        return commentId.value
    }

    @Transactional
    fun deleteComment(command: DeleteCommentCommand) {
        val userId = currentUser.require()
        val article =
            articleWriteRepository.findBySlug(Slug(command.articleSlug))
                ?: throw NotFoundException("Article not found")

        val typedCommentId = CommentId(command.commentId)
        val comment =
            commentWriteRepository.findById(typedCommentId)
                ?: throw NotFoundException("Comment not found")

        if (comment.articleId != article.id) {
            throw NotFoundException("Comment not found for this article")
        }

        if (!comment.canBeDeletedBy(userId)) {
            throw ForbiddenException("You can only delete your own comments")
        }
        commentWriteRepository.deleteById(typedCommentId)
        logger.info("Comment deleted: commentId={}", command.commentId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CommentCommands::class.java)
    }
}
