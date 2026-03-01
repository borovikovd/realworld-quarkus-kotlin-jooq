package com.example.comment

import com.example.article.ArticleRepository
import com.example.shared.exceptions.NotFoundException
import com.example.shared.security.SecurityContext
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class CommentService(
    private val commentRepository: CommentRepository,
    private val articleRepository: ArticleRepository,
    private val securityContext: SecurityContext,
) {
    @Transactional
    fun addComment(
        articleSlug: String,
        body: String,
    ): CommentId {
        val userId = securityContext.requireCurrentUserId()
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
        return commentId
    }

    @Transactional
    fun deleteComment(
        articleSlug: String,
        commentId: Long,
    ) {
        val userId = securityContext.requireCurrentUserId()
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

        comment.ensureCanBeDeletedBy(userId)
        commentRepository.deleteById(typedCommentId)
    }
}
