package com.example.application.command

import com.example.application.outport.ArticleWriteRepository
import com.example.application.outport.CommentWriteRepository
import com.example.application.outport.CurrentUser
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
    fun addComment(
        articleSlug: String,
        body: String,
    ): Long {
        if (body.isBlank()) {
            throw ValidationException(mapOf("body" to listOf("must not be blank")))
        }

        val userId = currentUser.require()
        val article =
            articleWriteRepository.findBySlug(Slug(articleSlug))
                ?: throw NotFoundException("Article not found")

        val commentId = commentWriteRepository.nextId()
        val comment =
            Comment(
                id = commentId,
                articleId = article.id,
                authorId = userId,
                body = body,
            )
        commentWriteRepository.create(comment)
        return commentId.value
    }

    @Transactional
    fun deleteComment(
        articleSlug: String,
        commentId: Long,
    ) {
        val userId = currentUser.require()
        val article =
            articleWriteRepository.findBySlug(Slug(articleSlug))
                ?: throw NotFoundException("Article not found")

        val typedCommentId = CommentId(commentId)
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
        logger.info("Comment deleted: commentId={}", commentId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CommentCommands::class.java)
    }
}
