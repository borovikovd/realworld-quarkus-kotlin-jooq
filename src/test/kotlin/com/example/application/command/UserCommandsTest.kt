package com.example.application.command

import com.example.application.port.outbound.Clock
import com.example.application.port.outbound.PasswordHashing
import com.example.application.port.outbound.UserWriteRepository
import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.PasswordHash
import com.example.domain.aggregate.user.User
import com.example.domain.aggregate.user.UserId
import com.example.domain.aggregate.user.Username
import com.example.domain.exception.UnauthorizedException
import com.example.domain.exception.ValidationException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserCommandsTest {
    private lateinit var userCommands: UserCommands
    private lateinit var userWriteRepository: UserWriteRepository
    private lateinit var passwordHashing: PasswordHashing
    private lateinit var clock: Clock

    @BeforeEach
    fun setup() {
        userWriteRepository = mockk()
        passwordHashing = mockk()
        clock = mockk()
        every { clock.now() } returns OffsetDateTime.now()
        userCommands = UserCommands(
            userWriteRepository = userWriteRepository,
            passwordHashing = passwordHashing,
            clock = clock,
        )
    }

    @Test
    fun `register should create user and return user id`() {
        val email = "test@example.com"
        val username = "testuser"
        val password = "password123"
        val passwordHash = "hashed-password"
        val userId = UserId(1L)

        every { userWriteRepository.existsByEmail(Email(email)) } returns false
        every { userWriteRepository.existsByUsername(Username(username)) } returns false
        every { passwordHashing.hash(password) } returns PasswordHash(passwordHash)
        every { userWriteRepository.nextId() } returns userId
        every { userWriteRepository.create(any()) } answers { firstArg() }

        val result = userCommands.register(email, username, password)

        assertEquals(userId.value, result)
        verify { userWriteRepository.existsByEmail(Email(email)) }
        verify { userWriteRepository.existsByUsername(Username(username)) }
        verify { passwordHashing.hash(password) }
        verify { userWriteRepository.nextId() }
        verify { userWriteRepository.create(any()) }
    }

    @Test
    fun `register should throw ValidationException when email is blank`() {
        every { userWriteRepository.existsByUsername(Username("testuser")) } returns false

        val exception =
            assertThrows<ValidationException> {
                userCommands.register("", "testuser", "password123")
            }

        assertEquals(listOf("must not be blank"), exception.errors["email"])
    }

    @Test
    fun `register should throw ValidationException when email has invalid format`() {
        every { userWriteRepository.existsByUsername(Username("testuser")) } returns false

        val exception =
            assertThrows<ValidationException> {
                userCommands.register("not-an-email", "testuser", "password123")
            }

        assertEquals(listOf("must be a valid email address"), exception.errors["email"])
    }

    @Test
    fun `register should throw ValidationException when username is blank`() {
        every { userWriteRepository.existsByEmail(Email("test@example.com")) } returns false

        val exception =
            assertThrows<ValidationException> {
                userCommands.register("test@example.com", "", "password123")
            }

        assertEquals(listOf("must not be blank"), exception.errors["username"])
    }

    @Test
    fun `register should throw ValidationException when username is too short`() {
        every { userWriteRepository.existsByEmail(Email("test@example.com")) } returns false

        val exception =
            assertThrows<ValidationException> {
                userCommands.register("test@example.com", "ab", "password123")
            }

        assertTrue(exception.errors["username"]!![0].contains("between"))
    }

    @Test
    fun `register should throw ValidationException when username is too long`() {
        every { userWriteRepository.existsByEmail(Email("test@example.com")) } returns false

        val exception =
            assertThrows<ValidationException> {
                userCommands.register("test@example.com", "a".repeat(51), "password123")
            }

        assertTrue(exception.errors["username"]!![0].contains("between"))
    }

    @Test
    fun `register should throw ValidationException when email already taken`() {
        val email = "taken@example.com"
        val username = "testuser"
        val password = "password123"

        every { userWriteRepository.existsByEmail(Email(email)) } returns true
        every { userWriteRepository.existsByUsername(Username(username)) } returns false

        val exception =
            assertThrows<ValidationException> {
                userCommands.register(email, username, password)
            }

        assertTrue(exception.errors.containsKey("email"))
        assertEquals(listOf("is already taken"), exception.errors["email"])
    }

    @Test
    fun `register should throw ValidationException when username already taken`() {
        val email = "test@example.com"
        val username = "takenuser"
        val password = "password123"

        every { userWriteRepository.existsByEmail(Email(email)) } returns false
        every { userWriteRepository.existsByUsername(Username(username)) } returns true

        val exception =
            assertThrows<ValidationException> {
                userCommands.register(email, username, password)
            }

        assertTrue(exception.errors.containsKey("username"))
        assertEquals(listOf("is already taken"), exception.errors["username"])
    }

    @Test
    fun `register should throw ValidationException when password too short`() {
        val email = "test@example.com"
        val username = "testuser"
        val password = "short"

        every { userWriteRepository.existsByEmail(Email(email)) } returns false
        every { userWriteRepository.existsByUsername(Username(username)) } returns false

        val exception =
            assertThrows<ValidationException> {
                userCommands.register(email, username, password)
            }

        assertTrue(exception.errors.containsKey("password"))
        assertEquals(listOf("must be at least 8 characters"), exception.errors["password"])
    }

    @Test
    fun `register should throw ValidationException with multiple errors`() {
        val email = "taken@example.com"
        val username = "takenuser"
        val password = "short"

        every { userWriteRepository.existsByEmail(Email(email)) } returns true
        every { userWriteRepository.existsByUsername(Username(username)) } returns true

        val exception =
            assertThrows<ValidationException> {
                userCommands.register(email, username, password)
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
                email = Email(email),
                username = Username("testuser"),
                passwordHash = PasswordHash("hashed-password"),
            )

        every { userWriteRepository.findByEmail(Email(email)) } returns user
        every { passwordHashing.verify(user.passwordHash, password) } returns true

        val result = userCommands.login(email, password)

        assertEquals(1L, result)
        verify { userWriteRepository.findByEmail(Email(email)) }
        verify { passwordHashing.verify(user.passwordHash, password) }
    }

    @Test
    fun `login should throw UnauthorizedException when user not found`() {
        val email = "nonexistent@example.com"
        val password = "password123"

        every { userWriteRepository.findByEmail(Email(email)) } returns null

        val exception =
            assertThrows<UnauthorizedException> {
                userCommands.login(email, password)
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
                email = Email(email),
                username = Username("testuser"),
                passwordHash = PasswordHash("hashed-password"),
            )

        every { userWriteRepository.findByEmail(Email(email)) } returns user
        every { passwordHashing.verify(user.passwordHash, password) } returns false

        val exception =
            assertThrows<UnauthorizedException> {
                userCommands.login(email, password)
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
                email = Email("old@example.com"),
                username = Username("olduser"),
                passwordHash = PasswordHash("old-hash"),
            )

        every { userWriteRepository.findById(userId) } returns existingUser
        every { userWriteRepository.existsByEmail(Email(newEmail)) } returns false
        every { userWriteRepository.existsByUsername(Username(newUsername)) } returns false
        every { passwordHashing.hash(newPassword) } returns PasswordHash(newPasswordHash)
        every { userWriteRepository.update(any()) } answers { firstArg() }

        val result = userCommands.updateUser(userId.value, newEmail, newUsername, newPassword, newBio, newImage)

        assertEquals(userId.value, result)
        verify { userWriteRepository.findById(userId) }
        verify { userWriteRepository.existsByEmail(Email(newEmail)) }
        verify { userWriteRepository.existsByUsername(Username(newUsername)) }
        verify { passwordHashing.hash(newPassword) }
        verify { userWriteRepository.update(any()) }
    }

    @Test
    fun `updateUser should keep existing values when nulls provided`() {
        val userId = UserId(1L)

        val existingUser =
            User(
                id = userId,
                email = Email("old@example.com"),
                username = Username("olduser"),
                passwordHash = PasswordHash("old-hash"),
            )

        every { userWriteRepository.findById(userId) } returns existingUser
        every { userWriteRepository.update(any()) } answers { firstArg() }

        val result = userCommands.updateUser(userId.value, null, null, null, null, null)

        assertEquals(userId.value, result)
        verify { userWriteRepository.findById(userId) }
        verify(exactly = 0) { passwordHashing.hash(any()) }
        verify { userWriteRepository.update(any()) }
    }

    @Test
    fun `updateUser should throw ValidationException when email is blank`() {
        val userId = UserId(1L)
        val existingUser =
            User(
                id = userId,
                email = Email("old@example.com"),
                username = Username("olduser"),
                passwordHash = PasswordHash("old-hash"),
            )

        every { userWriteRepository.findById(userId) } returns existingUser

        val exception =
            assertThrows<ValidationException> {
                userCommands.updateUser(userId.value, "", null, null, null, null)
            }

        assertEquals(listOf("must not be blank"), exception.errors["email"])
    }

    @Test
    fun `updateUser should throw ValidationException when email has invalid format`() {
        val userId = UserId(1L)
        val existingUser =
            User(
                id = userId,
                email = Email("old@example.com"),
                username = Username("olduser"),
                passwordHash = PasswordHash("old-hash"),
            )

        every { userWriteRepository.findById(userId) } returns existingUser

        val exception =
            assertThrows<ValidationException> {
                userCommands.updateUser(userId.value, "not-an-email", null, null, null, null)
            }

        assertEquals(listOf("must be a valid email address"), exception.errors["email"])
    }

    @Test
    fun `updateUser should throw ValidationException when username is too short`() {
        val userId = UserId(1L)
        val existingUser =
            User(
                id = userId,
                email = Email("old@example.com"),
                username = Username("olduser"),
                passwordHash = PasswordHash("old-hash"),
            )

        every { userWriteRepository.findById(userId) } returns existingUser

        val exception =
            assertThrows<ValidationException> {
                userCommands.updateUser(userId.value, null, "ab", null, null, null)
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
                email = Email("old@example.com"),
                username = Username("olduser"),
                passwordHash = PasswordHash("old-hash"),
            )

        every { userWriteRepository.findById(userId) } returns existingUser
        every { userWriteRepository.existsByEmail(Email(newEmail)) } returns true

        val exception =
            assertThrows<ValidationException> {
                userCommands.updateUser(userId.value, newEmail, null, null, null, null)
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
                email = Email("old@example.com"),
                username = Username("olduser"),
                passwordHash = PasswordHash("old-hash"),
            )

        every { userWriteRepository.findById(userId) } returns existingUser
        every { userWriteRepository.existsByUsername(Username(newUsername)) } returns true

        val exception =
            assertThrows<ValidationException> {
                userCommands.updateUser(userId.value, null, newUsername, null, null, null)
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
                email = Email("old@example.com"),
                username = Username("olduser"),
                passwordHash = PasswordHash("old-hash"),
            )

        every { userWriteRepository.findById(userId) } returns existingUser

        val exception =
            assertThrows<ValidationException> {
                userCommands.updateUser(userId.value, null, null, newPassword, null, null)
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
                email = Email(email),
                username = Username(username),
                passwordHash = PasswordHash("hash"),
            )

        every { userWriteRepository.findById(userId) } returns existingUser
        every { userWriteRepository.update(any()) } answers { firstArg() }

        val result = userCommands.updateUser(userId.value, email, username, null, null, null)

        assertEquals(userId.value, result)
    }
}
