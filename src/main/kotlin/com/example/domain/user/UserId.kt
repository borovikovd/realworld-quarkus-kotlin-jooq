package com.example.domain.user

import com.example.domain.shared.ValueObject

@ValueObject
@JvmInline
value class UserId(
    val value: Long,
)
