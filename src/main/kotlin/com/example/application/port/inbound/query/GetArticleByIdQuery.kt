package com.example.application.port.inbound.query

data class GetArticleByIdQuery(
    val id: Long,
    val viewerId: Long?,
)
