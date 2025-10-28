package com.example.user

import com.example.shared.exceptions.UnauthorizedException
import com.example.shared.exceptions.ValidationException
import com.example.shared.security.PasswordHasher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserServiceTest {
    private lateinit var userService: UserService
    private lateinit var userRepository: UserRepository
    private lateinit var passwordHasher: PasswordHasher

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        passwordHasher = mockk()
        userService = UserService()
        userService.userRepository = userRepository
        userService.passwordHasher = passwordHasher
    }

    @Test
    fun `register should create user when valid`() {
        val email = "test@example.com"
        val username = "testuser"
        val password = "password123"
        val passwordHash = "hashed-password"

        every { userRepository.existsByEmail(email) } returns false
        every { userRepository.existsByUsername(username) } returns false
        every { passwordHasher.hash(password) } returns passwordHash

        val expectedUser =
            User(
                id = 1L,
                email = email,
                username = username,
                passwordHash = passwordHash,
            )

        every { userRepository.create(any()) } returns expectedUser

        val result = userService.register(email, username, password)

        assertEquals(expectedUser, result)
        verify { userRepository.existsByEmail(email) }
        verify { userRepository.existsByUsername(username) }
        verify { passwordHasher.hash(password) }
        verify { userRepository.create(any()) }
    }

    @Test
    fun `register should throw ValidationException when email already taken`() {
        val email = "taken@example.com"
        val username = "testuser"
        val password = "password123"

        every { userRepository.existsByEmail(email) } returns true
        every { userRepository.existsByUsername(username) } returns false

        val exception =
            assertThrows<ValidationException> {
                userService.register(email, username, password)
            }

        assertTrue(exception.errors.containsKey("email"))
        assertEquals(listOf("is already taken"), exception.errors["email"])
    }

    @Test
    fun `register should throw ValidationException when username already taken`() {
        val email = "test@example.com"
        val username = "takenuser"
        val password = "password123"

        every { userRepository.existsByEmail(email) } returns false
        every { userRepository.existsByUsername(username) } returns true

        val exception =
            assertThrows<ValidationException> {
                userService.register(email, username, password)
            }

        assertTrue(exception.errors.containsKey("username"))
        assertEquals(listOf("is already taken"), exception.errors["username"])
    }

    @Test
    fun `register should throw ValidationException when password too short`() {
        val email = "test@example.com"
        val username = "testuser"
        val password = "short"

        every { userRepository.existsByEmail(email) } returns false
        every { userRepository.existsByUsername(username) } returns false

        val exception =
            assertThrows<ValidationException> {
                userService.register(email, username, password)
            }

        assertTrue(exception.errors.containsKey("password"))
        assertEquals(listOf("must be at least 8 characters"), exception.errors["password"])
    }

    @Test
    fun `register should throw ValidationException with multiple errors`() {
        val email = "taken@example.com"
        val username = "takenuser"
        val password = "short"

        every { userRepository.existsByEmail(email) } returns true
        every { userRepository.existsByUsername(username) } returns true

        val exception =
            assertThrows<ValidationException> {
                userService.register(email, username, password)
            }

        assertEquals(3, exception.errors.size)
        assertTrue(exception.errors.containsKey("email"))
        assertTrue(exception.errors.containsKey("username"))
        assertTrue(exception.errors.containsKey("password"))
    }

    @Test
    fun `login should return user when credentials valid`() {
        val email = "test@example.com"
        val password = "password123"

        val user =
            User(
                id = 1L,
                email = email,
                username = "testuser",
                passwordHash = "hashed-password",
            )

        every { userRepository.findByEmail(email) } returns user
        every { passwordHasher.verify(user.passwordHash, password) } returns true

        val result = userService.login(email, password)

        assertEquals(user, result)
        verify { userRepository.findByEmail(email) }
        verify { passwordHasher.verify(user.passwordHash, password) }
    }

    @Test
    fun `login should throw UnauthorizedException when user not found`() {
        val email = "nonexistent@example.com"
        val password = "password123"

        every { userRepository.findByEmail(email) } returns null

        val exception =
            assertThrows<UnauthorizedException> {
                userService.login(email, password)
            }

        assertEquals("Invalid email or password", exception.message)
    }

    @Test
    fun `login should throw UnauthorizedException when password invalid`() {
        val email = "test@example.com"
        val password = "wrongpassword"

        val user =
            User(
                id = 1L,
                email = email,
                username = "testuser",
                passwordHash = "hashed-password",
            )

        every { userRepository.findByEmail(email) } returns user
        every { passwordHasher.verify(user.passwordHash, password) } returns false

        val exception =
            assertThrows<UnauthorizedException> {
                userService.login(email, password)
            }

        assertEquals("Invalid email or password", exception.message)
    }

    @Test
    fun `updateUser should update all fields when valid`() {
        val userId = 1L
        val newEmail = "new@example.com"
        val newUsername = "newuser"
        val newPassword = "newpassword123"
        val newBio = "New bio"
        val newImage = "https://example.com/new.jpg"
        val newPasswordHash = "new-hashed-password"

        val existingUser =
            User(
                id = userId,
                email = "old@example.com",
                username = "olduser",
                passwordHash = "old-hash",
            )

        every { userRepository.findById(userId) } returns existingUser
        every { userRepository.existsByEmail(newEmail) } returns false
        every { userRepository.existsByUsername(newUsername) } returns false
        every { passwordHasher.hash(newPassword) } returns newPasswordHash

        val updatedUser =
            existingUser
                .updateProfile(newEmail, newUsername, newBio, newImage)
                .updatePassword(newPasswordHash)

        every { userRepository.update(any()) } returns updatedUser

        val result = userService.updateUser(userId, newEmail, newUsername, newPassword, newBio, newImage)

        assertEquals(updatedUser, result)
        verify { userRepository.findById(userId) }
        verify { userRepository.existsByEmail(newEmail) }
        verify { userRepository.existsByUsername(newUsername) }
        verify { passwordHasher.hash(newPassword) }
        verify { userRepository.update(any()) }
    }

    @Test
    fun `updateUser should keep existing values when nulls provided`() {
        val userId = 1L

        val existingUser =
            User(
                id = userId,
                email = "old@example.com",
                username = "olduser",
                passwordHash = "old-hash",
            )

        every { userRepository.findById(userId) } returns existingUser

        val updatedUser = existingUser.updateProfile(null, null, null, null)

        every { userRepository.update(any()) } returns updatedUser

        val result = userService.updateUser(userId, null, null, null, null, null)

        assertEquals(updatedUser, result)
        verify { userRepository.findById(userId) }
        verify(exactly = 0) { userRepository.existsByEmail(any()) }
        verify(exactly = 0) { userRepository.existsByUsername(any()) }
        verify(exactly = 0) { passwordHasher.hash(any()) }
        verify { userRepository.update(any()) }
    }

    @Test
    fun `updateUser should throw ValidationException when email already taken`() {
        val userId = 1L
        val newEmail = "taken@example.com"

        val existingUser =
            User(
                id = userId,
                email = "old@example.com",
                username = "olduser",
                passwordHash = "old-hash",
            )

        every { userRepository.findById(userId) } returns existingUser
        every { userRepository.existsByEmail(newEmail) } returns true

        val exception =
            assertThrows<ValidationException> {
                userService.updateUser(userId, newEmail, null, null, null, null)
            }

        assertTrue(exception.errors.containsKey("email"))
    }

    @Test
    fun `updateUser should throw ValidationException when username already taken`() {
        val userId = 1L
        val newUsername = "takenuser"

        val existingUser =
            User(
                id = userId,
                email = "old@example.com",
                username = "olduser",
                passwordHash = "old-hash",
            )

        every { userRepository.findById(userId) } returns existingUser
        every { userRepository.existsByUsername(newUsername) } returns true

        val exception =
            assertThrows<ValidationException> {
                userService.updateUser(userId, null, newUsername, null, null, null)
            }

        assertTrue(exception.errors.containsKey("username"))
    }

    @Test
    fun `updateUser should throw ValidationException when password too short`() {
        val userId = 1L
        val newPassword = "short"

        val existingUser =
            User(
                id = userId,
                email = "old@example.com",
                username = "olduser",
                passwordHash = "old-hash",
            )

        every { userRepository.findById(userId) } returns existingUser

        val exception =
            assertThrows<ValidationException> {
                userService.updateUser(userId, null, null, newPassword, null, null)
            }

        assertTrue(exception.errors.containsKey("password"))
    }

    @Test
    fun `updateUser should allow same email and username`() {
        val userId = 1L
        val email = "test@example.com"
        val username = "testuser"

        val existingUser =
            User(
                id = userId,
                email = email,
                username = username,
                passwordHash = "hash",
            )

        every { userRepository.findById(userId) } returns existingUser

        val updatedUser = existingUser.updateProfile(email, username, null, null)

        every { userRepository.update(any()) } returns updatedUser

        val result = userService.updateUser(userId, email, username, null, null, null)

        assertEquals(updatedUser, result)
        verify(exactly = 0) { userRepository.existsByEmail(any()) }
        verify(exactly = 0) { userRepository.existsByUsername(any()) }
    }

    @Test
    fun `getCurrentUser should return user when found`() {
        val userId = 1L

        val user =
            User(
                id = userId,
                email = "test@example.com",
                username = "testuser",
                passwordHash = "hash",
            )

        every { userRepository.findById(userId) } returns user

        val result = userService.getCurrentUser(userId)

        assertEquals(user, result)
        verify { userRepository.findById(userId) }
    }

    @Test
    fun `getCurrentUser should throw UnauthorizedException when user not found`() {
        val userId = 1L

        every { userRepository.findById(userId) } returns null

        val exception =
            assertThrows<UnauthorizedException> {
                userService.getCurrentUser(userId)
            }

        assertEquals("User not found", exception.message)
    }
}
