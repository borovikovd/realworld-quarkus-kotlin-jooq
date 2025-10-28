package com.example.shared.security

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordHasherTest {
    private val passwordHasher = PasswordHasher()

    @Test
    fun `should hash password`() {
        val password = "mySecurePassword123"
        val hash = passwordHasher.hash(password)

        assertNotEquals(password, hash)
        assertTrue(hash.isNotBlank())
    }

    @Test
    fun `should generate different hashes for same password`() {
        val password = "mySecurePassword123"
        val hash1 = passwordHasher.hash(password)
        val hash2 = passwordHasher.hash(password)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `should verify correct password`() {
        val password = "mySecurePassword123"
        val hash = passwordHasher.hash(password)

        assertTrue(passwordHasher.verify(hash, password))
    }

    @Test
    fun `should reject incorrect password`() {
        val password = "mySecurePassword123"
        val wrongPassword = "wrongPassword"
        val hash = passwordHasher.hash(password)

        assertFalse(passwordHasher.verify(hash, wrongPassword))
    }

    @Test
    fun `should reject empty password`() {
        val password = "mySecurePassword123"
        val hash = passwordHasher.hash(password)

        assertFalse(passwordHasher.verify(hash, ""))
    }

    @Test
    fun `should handle special characters`() {
        val password = "p@ssw0rd!#\$%^&*()"
        val hash = passwordHasher.hash(password)

        assertTrue(passwordHasher.verify(hash, password))
    }

    @Test
    fun `should handle unicode characters`() {
        val password = "пароль123密码"
        val hash = passwordHasher.hash(password)

        assertTrue(passwordHasher.verify(hash, password))
    }

    @Test
    fun `should handle long passwords`() {
        val password = "a".repeat(100)
        val hash = passwordHasher.hash(password)

        assertTrue(passwordHasher.verify(hash, password))
    }
}
