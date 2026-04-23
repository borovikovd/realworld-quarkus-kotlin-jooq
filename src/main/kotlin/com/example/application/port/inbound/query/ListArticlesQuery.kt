package com.example.application.port.inbound.query

data class ListArticlesQuery(
    val tag: String?,
    val author: String?,
    val favorited: String?,
    val limit: Int,
    val offset: Int,
    val viewerId: Long?,
)
