package com.example.application.comment

import com.example.application.CurrentUser
import com.example.domain.article.ArticleRepository
import com.example.domain.comment.Comment
import com.example.domain.comment.CommentId
import com.example.domain.comment.CommentRepository
import com.example.domain.shared.ForbiddenException
import com.example.domain.shared.NotFoundException
import com.example.domain.shared.ValidationException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory

@ApplicationScoped
class DefaultCommentWriteService(
    private val commentRepository: CommentRepository,
    private val articleRepository: ArticleRepository,
    private val currentUser: CurrentUser,
) : CommentWriteService {
    companion object {
        private val logger = LoggerFactory.getLogger(DefaultCommentWriteService::class.java)
    }

    @Transactional
    override fun addComment(
        articleSlug: String,
        body: String,
    ): Long {
        if (body.isBlank()) {
            throw ValidationException(mapOf("body" to listOf("must not be blank")))
        }

        val userId = currentUser.require()
        val article =
            articleRepository.findBySlug(articleSlug)
                ?: throw NotFoundException("Article not found")

        val commentId = commentRepository.nextId()
        val comment =
            Comment(
                id = commentId,
                articleId = article.id,
                authorId = userId,
                body = body,
            )
        commentRepository.create(comment)
        return commentId.value
    }

    @Transactional
    override fun deleteComment(
        articleSlug: String,
        commentId: Long,
    ) {
        val userId = currentUser.require()
        val article =
            articleRepository.findBySlug(articleSlug)
                ?: throw NotFoundException("Article not found")

        val typedCommentId = CommentId(commentId)
        val comment =
            commentRepository.findById(typedCommentId)
                ?: throw NotFoundException("Comment not found")

        if (comment.articleId != article.id) {
            throw NotFoundException("Comment not found for this article")
        }

        if (!comment.canBeDeletedBy(userId)) {
            throw ForbiddenException("You can only delete your own comments")
        }
        commentRepository.deleteById(typedCommentId)
        logger.info("Comment deleted: commentId={}", commentId)
    }
}
