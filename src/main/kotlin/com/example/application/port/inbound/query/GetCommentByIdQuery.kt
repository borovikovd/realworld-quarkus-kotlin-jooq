package com.example.application.port.inbound.query

data class GetCommentByIdQuery(
    val id: Long,
    val viewerId: Long?,
)
