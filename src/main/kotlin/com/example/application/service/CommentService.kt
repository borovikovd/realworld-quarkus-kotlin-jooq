package com.example.application.service

import com.example.application.port.ArticleRepository
import com.example.application.port.CommentRepository
import com.example.application.port.security.CurrentUser
import com.example.application.readmodel.CommentReadModel
import com.example.application.usecase.CommentCommands
import com.example.application.usecase.CommentQueries
import com.example.domain.aggregate.article.Slug
import com.example.domain.aggregate.comment.Body
import com.example.domain.aggregate.comment.Comment
import com.example.domain.aggregate.comment.CommentId
import com.example.domain.aggregate.user.UserId
import com.example.domain.exception.ForbiddenException
import com.example.domain.exception.NotFoundException
import com.example.domain.exception.ValidationException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory

@ApplicationScoped
class CommentService(
    private val commentRepository: CommentRepository,
    private val articleRepository: ArticleRepository,
    private val currentUser: CurrentUser,
) : CommentCommands,
    CommentQueries {
    @Transactional
    override fun addComment(
        articleSlug: String,
        body: String,
    ): CommentId {
        val bodyVo =
            runCatching { Body(body) }
                .getOrElse { throw ValidationException(mapOf("body" to listOf("must not be blank"))) }

        val userId = currentUser.require()
        val article =
            articleRepository.findBySlug(Slug(articleSlug))
                ?: throw NotFoundException("Article not found")

        val commentId = commentRepository.nextId()
        val comment =
            Comment(
                id = commentId,
                articleId = article.id,
                authorId = userId,
                body = bodyVo,
            )
        commentRepository.create(comment)
        return commentId
    }

    @Transactional
    override fun deleteComment(
        articleSlug: String,
        commentId: CommentId,
    ) {
        val userId = currentUser.require()
        val article =
            articleRepository.findBySlug(Slug(articleSlug))
                ?: throw NotFoundException("Article not found")

        val comment =
            commentRepository.findById(commentId)
                ?: throw NotFoundException("Comment not found")

        if (comment.articleId != article.id) {
            throw NotFoundException("Comment not found for this article")
        }

        if (!comment.canBeDeletedBy(userId)) {
            throw ForbiddenException("You can only delete your own comments")
        }
        commentRepository.deleteById(commentId)
        logger.info("Comment deleted: commentId={}", commentId.value)
    }

    override fun getCommentById(
        id: CommentId,
        viewerId: UserId?,
    ): CommentReadModel? = commentRepository.findById(id, viewerId)

    override fun getCommentsBySlug(
        slug: String,
        viewerId: UserId?,
    ): List<CommentReadModel> = commentRepository.findByArticleSlug(slug, viewerId)

    companion object {
        private val logger = LoggerFactory.getLogger(CommentService::class.java)
    }
}
