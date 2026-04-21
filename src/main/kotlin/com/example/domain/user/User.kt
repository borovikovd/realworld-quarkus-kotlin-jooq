package com.example.domain.user

import com.example.domain.shared.AggregateRoot
import com.example.domain.shared.Entity
import java.time.OffsetDateTime

@AggregateRoot
class User(
    override val id: UserId,
    val email: Email,
    val username: Username,
    val passwordHash: PasswordHash,
    val bio: String? = null,
    val image: String? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
) : Entity<UserId>() {
    fun updateProfile(
        updatedAt: OffsetDateTime,
        email: Email? = null,
        username: Username? = null,
        bio: String? = null,
        image: String? = null,
    ): User =
        User(
            id = id,
            email = email ?: this.email,
            username = username ?: this.username,
            passwordHash = passwordHash,
            bio = bio ?: this.bio,
            image = image ?: this.image,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    fun updatePassword(
        newPasswordHash: PasswordHash,
        updatedAt: OffsetDateTime,
    ): User =
        User(
            id = id,
            email = email,
            username = username,
            passwordHash = newPasswordHash,
            bio = bio,
            image = image,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    override fun toString(): String = "User(id=$id, email=$email, username=$username)"
}
