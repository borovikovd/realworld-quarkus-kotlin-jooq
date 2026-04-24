package com.example.application.query

import com.example.application.outport.CommentReadRepository
import com.example.application.inport.query.readmodel.CommentReadModel
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class CommentQueries(
    private val commentReadRepository: CommentReadRepository,
) {
    fun getCommentById(
        id: Long,
        viewerId: Long?,
    ): CommentReadModel? = commentReadRepository.findById(id, viewerId)

    fun getCommentsBySlug(
        slug: String,
        viewerId: Long?,
    ): List<CommentReadModel> = commentReadRepository.findByArticleSlug(slug, viewerId)
}
