package com.example.application.inport.query.readmodel

import java.time.OffsetDateTime

data class CommentReadModel(
    val id: Long,
    val body: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val author: ProfileReadModel,
)
