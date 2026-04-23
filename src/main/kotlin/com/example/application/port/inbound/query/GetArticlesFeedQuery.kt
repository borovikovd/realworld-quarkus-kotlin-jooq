package com.example.application.port.inbound.query

data class GetArticlesFeedQuery(
    val viewerId: Long,
    val limit: Int,
    val offset: Int,
)
