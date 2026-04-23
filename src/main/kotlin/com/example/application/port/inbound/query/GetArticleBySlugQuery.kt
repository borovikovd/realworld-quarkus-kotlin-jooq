package com.example.application.port.inbound.query

data class GetArticleBySlugQuery(
    val slug: String,
    val viewerId: Long?,
)
