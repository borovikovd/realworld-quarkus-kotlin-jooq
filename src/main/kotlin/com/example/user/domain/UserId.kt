package com.example.user.domain

import com.example.domain.shared.ValueObject

@ValueObject
@JvmInline
value class UserId(
    val value: Long,
)
