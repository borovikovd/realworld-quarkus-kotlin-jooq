package com.example.application.port.inbound.query

data class GetCommentsBySlugQuery(
    val slug: String,
    val viewerId: Long?,
)
