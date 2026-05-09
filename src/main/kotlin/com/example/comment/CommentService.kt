package com.example.comment

import com.example.article.ArticleRepository
import com.example.common.security.CurrentUser
import com.example.common.web.ForbiddenException
import com.example.common.web.NotFoundException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory

@ApplicationScoped
class CommentService(
    private val commentRepository: CommentRepository,
    private val articleRepository: ArticleRepository,
    private val currentUser: CurrentUser,
) {
    @Transactional
    fun add(
        articleSlug: String,
        body: String,
    ): CommentDto {
        val userId = currentUser.require()
        val articleId =
            articleRepository.findIdBySlug(articleSlug)
                ?: throw NotFoundException("Article not found")

        val commentId = commentRepository.nextId()
        commentRepository.insert(
            Comment(
                id = commentId,
                articleId = articleId,
                authorId = userId,
                body = body,
            ),
        )

        return commentRepository.findDtoById(commentId, userId)
            ?: error("Comment not found after insert: id=${commentId.value}")
    }

    @Transactional
    fun delete(
        articleSlug: String,
        commentId: CommentId,
    ) {
        val userId = currentUser.require()
        val articleId =
            articleRepository.findIdBySlug(articleSlug)
                ?: throw NotFoundException("Article not found")

        val comment =
            commentRepository.findById(commentId)
                ?: throw NotFoundException("Comment not found")

        if (comment.articleId != articleId) throw NotFoundException("Comment not found for this article")
        if (comment.authorId != userId) throw ForbiddenException("You can only delete your own comments")

        commentRepository.deleteById(commentId)
        logger.info("Comment deleted: commentId={}", commentId.value)
    }

    fun listByArticle(articleSlug: String): List<CommentDto> =
        commentRepository.findDtosByArticleSlug(articleSlug, currentUser.id)

    companion object {
        private val logger = LoggerFactory.getLogger(CommentService::class.java)
    }
}
