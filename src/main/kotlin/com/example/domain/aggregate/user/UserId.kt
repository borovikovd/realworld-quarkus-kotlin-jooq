package com.example.domain.aggregate.user

import com.example.domain.ValueObject

@ValueObject
@JvmInline
value class UserId(
    val value: Long,
)
