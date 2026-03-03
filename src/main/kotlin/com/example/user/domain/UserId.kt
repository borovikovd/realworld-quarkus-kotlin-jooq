package com.example.user.domain

import com.example.shared.architecture.ValueObject

@ValueObject
@JvmInline
value class UserId(
    val value: Long,
)
