package com.example.user.domain

import com.example.domain.shared.AggregateRoot
import com.example.domain.shared.Entity
import java.time.OffsetDateTime

@AggregateRoot
class User(
    override val id: UserId,
    val email: String,
    val username: String,
    val passwordHash: String,
    val bio: String? = null,
    val image: String? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
) : Entity<UserId>() {
    init {
        require(email.isNotBlank()) { "Email must not be blank" }
        require(EMAIL_REGEX.matches(email)) { "Email must be a valid email address" }
        require(username.isNotBlank()) { "Username must not be blank" }
        require(username.length in MIN_USERNAME_LENGTH..MAX_USERNAME_LENGTH) {
            "Username must be between $MIN_USERNAME_LENGTH and $MAX_USERNAME_LENGTH characters"
        }
    }

    companion object {
        internal val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        internal const val MIN_USERNAME_LENGTH = 3
        internal const val MAX_USERNAME_LENGTH = 50
    }

    fun updateProfile(
        email: String? = null,
        username: String? = null,
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
            updatedAt = OffsetDateTime.now(),
        )

    fun updatePassword(newPasswordHash: String): User =
        User(
            id = id,
            email = email,
            username = username,
            passwordHash = newPasswordHash,
            bio = bio,
            image = image,
            createdAt = createdAt,
            updatedAt = OffsetDateTime.now(),
        )

    override fun toString(): String = "User(id=$id, email=$email, username=$username)"
}
