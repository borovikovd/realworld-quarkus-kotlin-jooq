package com.example.user

import com.example.common.security.PasswordHash
import java.time.OffsetDateTime

@JvmInline
value class UserId(
    val value: Long,
)

data class User(
    val id: UserId,
    val email: String,
    val username: String,
    val passwordHash: PasswordHash,
    val bio: String? = null,
    val image: String? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
