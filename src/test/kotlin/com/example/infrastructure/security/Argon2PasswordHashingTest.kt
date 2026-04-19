package com.example.infrastructure.security

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class Argon2PasswordHashingTest {
    private val passwordHashing = Argon2PasswordHashing()

    @Test
    fun `should hash password`() {
        val password = "mySecurePassword123"
        val hash = passwordHashing.hash(password)

        assertNotEquals(password, hash.value)
        assertTrue(hash.value.isNotBlank())
    }

    @Test
    fun `should generate different hashes for same password`() {
        val password = "mySecurePassword123"
        val hash1 = passwordHashing.hash(password)
        val hash2 = passwordHashing.hash(password)

        assertNotEquals(hash1.value, hash2.value)
    }

    @Test
    fun `should verify correct password`() {
        val password = "mySecurePassword123"
        val hash = passwordHashing.hash(password)

        assertTrue(passwordHashing.verify(hash, password))
    }

    @Test
    fun `should reject incorrect password`() {
        val password = "mySecurePassword123"
        val wrongPassword = "wrongPassword"
        val hash = passwordHashing.hash(password)

        assertFalse(passwordHashing.verify(hash, wrongPassword))
    }

    @Test
    fun `should reject empty password`() {
        val password = "mySecurePassword123"
        val hash = passwordHashing.hash(password)

        assertFalse(passwordHashing.verify(hash, ""))
    }

    @Test
    fun `should handle special characters`() {
        val password = "p@ssw0rd!#\$%^&*()"
        val hash = passwordHashing.hash(password)

        assertTrue(passwordHashing.verify(hash, password))
    }

    @Test
    fun `should handle unicode characters`() {
        val password = "пароль123密码"
        val hash = passwordHashing.hash(password)

        assertTrue(passwordHashing.verify(hash, password))
    }

    @Test
    fun `should handle long passwords`() {
        val password = "a".repeat(100)
        val hash = passwordHashing.hash(password)

        assertTrue(passwordHashing.verify(hash, password))
    }
}
