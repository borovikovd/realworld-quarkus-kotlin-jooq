package com.example.domain.user

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserTest {
    @Test
    fun `should create valid user`() {
        val user =
            User(
                id = UserId(1L),
                email = Email("test@example.com"),
                username = Username("testuser"),
                passwordHash = PasswordHash("hashed-password"),
                bio = "Test bio",
                image = "https://example.com/image.jpg",
            )

        assertEquals("test@example.com", user.email.value)
        assertEquals("testuser", user.username.value)
        assertEquals("hashed-password", user.passwordHash.value)
        assertEquals("Test bio", user.bio)
        assertEquals("https://example.com/image.jpg", user.image)
    }

    @Test
    fun `should fail when email is blank`() {
        val exception = assertThrows<IllegalArgumentException> { Email("") }
        assertEquals("Email must not be blank", exception.message)
    }

    @Test
    fun `should fail when email is invalid`() {
        val exception = assertThrows<IllegalArgumentException> { Email("invalid-email") }
        assertEquals("Email must be a valid email address", exception.message)
    }

    @Test
    fun `should fail when username is blank`() {
        val exception = assertThrows<IllegalArgumentException> { Username("") }
        assertEquals("Username must not be blank", exception.message)
    }

    @Test
    fun `should fail when username is too short`() {
        val exception = assertThrows<IllegalArgumentException> { Username("ab") }
        assertEquals("Username must be between 3 and 50 characters", exception.message)
    }

    @Test
    fun `should fail when username is too long`() {
        val exception = assertThrows<IllegalArgumentException> { Username("a".repeat(51)) }
        assertEquals("Username must be between 3 and 50 characters", exception.message)
    }

    @Test
    fun `should update profile with all fields`() {
        val originalUser =
            User(
                id = UserId(1L),
                email = Email("original@example.com"),
                username = Username("original"),
                passwordHash = PasswordHash("hashed-password"),
                bio = "Original bio",
                image = "https://example.com/original.jpg",
                updatedAt = OffsetDateTime.now().minusDays(1),
            )

        val updatedUser =
            originalUser.updateProfile(
                updatedAt = OffsetDateTime.now(),
                email = Email("updated@example.com"),
                username = Username("updated"),
                bio = "Updated bio",
                image = "https://example.com/updated.jpg",
            )

        assertEquals("updated@example.com", updatedUser.email.value)
        assertEquals("updated", updatedUser.username.value)
        assertEquals("Updated bio", updatedUser.bio)
        assertEquals("https://example.com/updated.jpg", updatedUser.image)
        assertTrue(updatedUser.updatedAt.isAfter(originalUser.updatedAt))
    }

    @Test
    fun `should keep existing email and username when not provided`() {
        val originalUser =
            User(
                id = UserId(1L),
                email = Email("original@example.com"),
                username = Username("original"),
                passwordHash = PasswordHash("hashed-password"),
            )

        val updatedUser =
            originalUser.updateProfile(
                updatedAt = OffsetDateTime.now(),
                bio = "New bio",
                image = "https://example.com/new.jpg",
            )

        assertEquals("original@example.com", updatedUser.email.value)
        assertEquals("original", updatedUser.username.value)
        assertEquals("New bio", updatedUser.bio)
        assertEquals("https://example.com/new.jpg", updatedUser.image)
    }

    @Test
    fun `should keep existing bio and image when null provided`() {
        val originalUser =
            User(
                id = UserId(1L),
                email = Email("test@example.com"),
                username = Username("testuser"),
                passwordHash = PasswordHash("hashed-password"),
                bio = "Original bio",
                image = "https://example.com/original.jpg",
            )

        val updatedUser =
            originalUser.updateProfile(
                updatedAt = OffsetDateTime.now(),
                bio = null,
                image = null,
            )

        assertEquals("Original bio", updatedUser.bio)
        assertEquals("https://example.com/original.jpg", updatedUser.image)
    }

    @Test
    fun `should update password and timestamp`() {
        val originalUser =
            User(
                id = UserId(1L),
                email = Email("test@example.com"),
                username = Username("testuser"),
                passwordHash = PasswordHash("old-hash"),
                updatedAt = OffsetDateTime.now().minusDays(1),
            )

        val updatedUser = originalUser.updatePassword(PasswordHash("new-hash"), OffsetDateTime.now())

        assertEquals("new-hash", updatedUser.passwordHash.value)
        assertTrue(updatedUser.updatedAt.isAfter(originalUser.updatedAt))
    }

    @Test
    fun `should have identity-based equality`() {
        val user1 =
            User(
                id = UserId(1L),
                email = Email("user1@example.com"),
                username = Username("user1"),
                passwordHash = PasswordHash("hash1"),
            )

        val user2 =
            User(
                id = UserId(1L),
                email = Email("user2@example.com"),
                username = Username("user2"),
                passwordHash = PasswordHash("hash2"),
            )

        assertEquals(user1, user2)
        assertEquals(user1.hashCode(), user2.hashCode())
    }
}
