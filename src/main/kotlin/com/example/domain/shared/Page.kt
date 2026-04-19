package com.example.domain.shared

data class Page<T>(
    val items: List<T>,
    val total: Long,
    val limit: Int,
    val offset: Int,
)
