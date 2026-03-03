package com.example.user.application

import com.example.shared.exceptions.UnauthorizedException
import com.example.shared.exceptions.ValidationException
import com.example.shared.security.PasswordHasher
import com.example.user.domain.User
import com.example.user.domain.UserId
import com.example.user.domain.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultUserWriteServiceTest {
    private lateinit var userWriteService: DefaultUserWriteService
    private lateinit var userRepository: UserRepository
    private lateinit var passwordHasher: PasswordHasher

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        passwordHasher = mockk()
        userWriteService = DefaultUserWriteService(
            userRepository = userRepository,
            passwordHasher = passwordHasher,
        )
    }

    @Test
    fun `register should create user and return user id`() {
        val email = "test@example.com"
        val username = "testuser"
        val password = "password123"
        val passwordHash = "hashed-password"
        val userId = UserId(1L)

        every { userRepository.existsByEmail(email) } returns false
        every { userRepository.existsByUsername(username) } returns false
        every { passwordHasher.hash(password) } returns passwordHash
        every { userRepository.nextId() } returns userId
        every { userRepository.create(any()) } answers { firstArg() }

        val result = userWriteService.register(email, username, password)

        assertEquals(userId.value, result)
        verify { userRepository.existsByEmail(email) }
        verify { userRepository.existsByUsername(username) }
        verify { passwordHasher.hash(password) }
        verify { userRepository.nextId() }
        verify { userRepository.create(any()) }
    }

    @Test
    fun `register should throw ValidationException when email is blank`() {
        every { userRepository.existsByUsername("testuser") } returns false

        val exception =
            assertThrows<ValidationException> {
                userWriteService.register("", "testuser", "password123")
            }

        assertEquals(listOf("must not be blank"), exception.errors["email"])
        verify(exactly = 0) { userRepository.existsByEmail(any()) }
    }

    @Test
    fun `register should throw ValidationException when email has invalid format`() {
        every { userRepository.existsByUsername("testuser") } returns false

        val exception =
            assertThrows<ValidationException> {
                userWriteService.register("not-an-email", "testuser", "password123")
            }

        assertEquals(listOf("must be a valid email address"), exception.errors["email"])
        verify(exactly = 0) { userRepository.existsByEmail(any()) }
    }

    @Test
    fun `register should throw ValidationException when username is blank`() {
        every { userRepository.existsByEmail("test@example.com") } returns false

        val exception =
            assertThrows<ValidationException> {
                userWriteService.register("test@example.com", "", "password123")
            }

        assertEquals(listOf("must not be blank"), exception.errors["username"])
        verify(exactly = 0) { userRepository.existsByUsername(any()) }
    }

    @Test
    fun `register should throw ValidationException when username is too short`() {
        every { userRepository.existsByEmail("test@example.com") } returns false

        val exception =
            assertThrows<ValidationException> {
                userWriteService.register("test@example.com", "ab", "password123")
            }

        assertTrue(exception.errors["username"]!![0].contains("between"))
        verify(exactly = 0) { userRepository.existsByUsername(any()) }
    }

    @Test
    fun `register should throw ValidationException when username is too long`() {
        every { userRepository.existsByEmail("test@example.com") } returns false

        val exception =
            assertThrows<ValidationException> {
                userWriteService.register("test@example.com", "a".repeat(51), "password123")
            }

        assertTrue(exception.errors["username"]!![0].contains("between"))
        verify(exactly = 0) { userRepository.existsByUsername(any()) }
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
                userWriteService.register(email, username, password)
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
                userWriteService.register(email, username, password)
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
                userWriteService.register(email, username, password)
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
                userWriteService.register(email, username, password)
            }

        assertEquals(3, exception.errors.size)
        assertTrue(exception.errors.containsKey("email"))
        assertTrue(exception.errors.containsKey("username"))
        assertTrue(exception.errors.containsKey("password"))
    }

    @Test
    fun `login should return user id when credentials valid`() {
        val email = "test@example.com"
        val password = "password123"

        val user =
            User(
                id = UserId(1L),
                email = email,
                username = "testuser",
                passwordHash = "hashed-password",
            )

        every { userRepository.findByEmail(email) } returns user
        every { passwordHasher.verify(user.passwordHash, password) } returns true

        val result = userWriteService.login(email, password)

        assertEquals(1L, result)
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
                userWriteService.login(email, password)
            }

        assertEquals("Invalid email or password", exception.message)
    }

    @Test
    fun `login should throw UnauthorizedException when password invalid`() {
        val email = "test@example.com"
        val password = "wrongpassword"

        val user =
            User(
                id = UserId(1L),
                email = email,
                username = "testuser",
                passwordHash = "hashed-password",
            )

        every { userRepository.findByEmail(email) } returns user
        every { passwordHasher.verify(user.passwordHash, password) } returns false

        val exception =
            assertThrows<UnauthorizedException> {
                userWriteService.login(email, password)
            }

        assertEquals("Invalid email or password", exception.message)
    }

    @Test
    fun `updateUser should update all fields and return user id`() {
        val userId = UserId(1L)
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
        every { userRepository.update(any()) } answers { firstArg() }

        val result = userWriteService.updateUser(userId.value, newEmail, newUsername, newPassword, newBio, newImage)

        assertEquals(userId.value, result)
        verify { userRepository.findById(userId) }
        verify { userRepository.existsByEmail(newEmail) }
        verify { userRepository.existsByUsername(newUsername) }
        verify { passwordHasher.hash(newPassword) }
        verify { userRepository.update(any()) }
    }

    @Test
    fun `updateUser should keep existing values when nulls provided`() {
        val userId = UserId(1L)

        val existingUser =
            User(
                id = userId,
                email = "old@example.com",
                username = "olduser",
                passwordHash = "old-hash",
            )

        every { userRepository.findById(userId) } returns existingUser
        every { userRepository.update(any()) } answers { firstArg() }

        val result = userWriteService.updateUser(userId.value, null, null, null, null, null)

        assertEquals(userId.value, result)
        verify { userRepository.findById(userId) }
        verify(exactly = 0) { userRepository.existsByEmail(any()) }
        verify(exactly = 0) { userRepository.existsByUsername(any()) }
        verify(exactly = 0) { passwordHasher.hash(any()) }
        verify { userRepository.update(any()) }
    }

    @Test
    fun `updateUser should throw ValidationException when email is blank`() {
        val userId = UserId(1L)
        val existingUser =
            User(id = userId, email = "old@example.com", username = "olduser", passwordHash = "old-hash")

        every { userRepository.findById(userId) } returns existingUser

        val exception =
            assertThrows<ValidationException> {
                userWriteService.updateUser(userId.value, "", null, null, null, null)
            }

        assertEquals(listOf("must not be blank"), exception.errors["email"])
    }

    @Test
    fun `updateUser should throw ValidationException when email has invalid format`() {
        val userId = UserId(1L)
        val existingUser =
            User(id = userId, email = "old@example.com", username = "olduser", passwordHash = "old-hash")

        every { userRepository.findById(userId) } returns existingUser

        val exception =
            assertThrows<ValidationException> {
                userWriteService.updateUser(userId.value, "not-an-email", null, null, null, null)
            }

        assertEquals(listOf("must be a valid email address"), exception.errors["email"])
    }

    @Test
    fun `updateUser should throw ValidationException when username is too short`() {
        val userId = UserId(1L)
        val existingUser =
            User(id = userId, email = "old@example.com", username = "olduser", passwordHash = "old-hash")

        every { userRepository.findById(userId) } returns existingUser

        val exception =
            assertThrows<ValidationException> {
                userWriteService.updateUser(userId.value, null, "ab", null, null, null)
            }

        assertTrue(exception.errors["username"]!![0].contains("between"))
    }

    @Test
    fun `updateUser should throw ValidationException when email already taken`() {
        val userId = UserId(1L)
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
                userWriteService.updateUser(userId.value, newEmail, null, null, null, null)
            }

        assertTrue(exception.errors.containsKey("email"))
    }

    @Test
    fun `updateUser should throw ValidationException when username already taken`() {
        val userId = UserId(1L)
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
                userWriteService.updateUser(userId.value, null, newUsername, null, null, null)
            }

        assertTrue(exception.errors.containsKey("username"))
    }

    @Test
    fun `updateUser should throw ValidationException when password too short`() {
        val userId = UserId(1L)
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
                userWriteService.updateUser(userId.value, null, null, newPassword, null, null)
            }

        assertTrue(exception.errors.containsKey("password"))
    }

    @Test
    fun `updateUser should allow same email and username`() {
        val userId = UserId(1L)
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
        every { userRepository.update(any()) } answers { firstArg() }

        val result = userWriteService.updateUser(userId.value, email, username, null, null, null)

        assertEquals(userId.value, result)
        verify(exactly = 0) { userRepository.existsByEmail(any()) }
        verify(exactly = 0) { userRepository.existsByUsername(any()) }
    }
}
