package com.example.shared.domain

data class Page<T>(
    val items: List<T>,
    val total: Long,
    val limit: Int,
    val offset: Int,
)
