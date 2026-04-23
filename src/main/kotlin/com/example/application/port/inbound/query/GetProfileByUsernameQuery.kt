package com.example.application.port.inbound.query

data class GetProfileByUsernameQuery(
    val username: String,
    val viewerId: Long?,
)
