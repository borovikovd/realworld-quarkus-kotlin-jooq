package com.example.application.port.inbound.query

data class CountArticlesQuery(
    val tag: String?,
    val author: String?,
    val favorited: String?,
)
