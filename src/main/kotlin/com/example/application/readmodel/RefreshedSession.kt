package com.example.application.readmodel

data class RefreshedSession(
    val userId: Long,
    val tokens: IssuedTokens,
)
