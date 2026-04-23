package com.example.application.query

import com.example.application.port.inbound.query.GetCommentByIdQuery
import com.example.application.port.inbound.query.GetCommentsBySlugQuery
import com.example.application.port.outbound.CommentReadModel

interface CommentQueries {
    fun getCommentById(query: GetCommentByIdQuery): CommentReadModel?

    fun getCommentsBySlug(query: GetCommentsBySlugQuery): List<CommentReadModel>
}
