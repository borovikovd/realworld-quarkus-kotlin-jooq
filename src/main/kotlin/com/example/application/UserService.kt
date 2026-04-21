package com.example.application

import com.example.domain.auth.PasswordHashing
import com.example.domain.shared.Clock
import com.example.domain.shared.UnauthorizedException
import com.example.domain.shared.ValidationException
import com.example.domain.user.Email
import com.example.domain.user.User
import com.example.domain.user.UserId
import com.example.domain.user.UserRepository
import com.example.domain.user.Username
import io.micrometer.core.annotation.Counted
import io.micrometer.core.annotation.Timed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory

@ApplicationScoped
class UserService(
    private val userRepository: UserRepository,
    private val passwordHashing: PasswordHashing,
    private val clock: Clock,
) {
    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private val logger = LoggerFactory.getLogger(UserService::class.java)
    }

    @Timed("user.registration")
    @Counted("user.registration.count")
    @Transactional
    fun register(
        email: String,
        username: String,
        password: String,
    ): Long {
        val errors = mutableMapOf<String, List<String>>()

        val emailVo = parseEmail(email, errors)
        if (emailVo != null && userRepository.existsByEmail(emailVo)) {
            errors["email"] = listOf("is already taken")
        }

        val usernameVo = parseUsername(username, errors)
        if (usernameVo != null && userRepository.existsByUsername(usernameVo)) {
            errors["username"] = listOf("is already taken")
        }

        if (password.length < MIN_PASSWORD_LENGTH) {
            errors["password"] = listOf("must be at least $MIN_PASSWORD_LENGTH characters")
        }

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }

        val userId = userRepository.nextId()
        val user =
            User(
                id = userId,
                email = emailVo!!,
                username = usernameVo!!,
                passwordHash = passwordHashing.hash(password),
            )
        userRepository.create(user)
        logger.info("User registered: userId={}, username={}", userId.value, username)
        return userId.value
    }

    @Timed("user.login")
    fun login(
        email: String,
        password: String,
    ): Long {
        val emailVo =
            runCatching { Email(email) }
                .getOrElse { throw UnauthorizedException("Invalid email or password") }

        val user = userRepository.findByEmail(emailVo)
        if (user == null) {
            logger.info("Login failed: invalid credentials")
            throw UnauthorizedException("Invalid email or password")
        }

        if (!passwordHashing.verify(user.passwordHash, password)) {
            logger.info("Login failed: invalid credentials")
            throw UnauthorizedException("Invalid email or password")
        }

        return user.id.value
    }

    @Transactional
    fun updateUser(
        userId: Long,
        email: String?,
        username: String?,
        password: String?,
        bio: String?,
        image: String?,
    ): Long {
        val typedUserId = UserId(userId)
        val user =
            userRepository.findById(typedUserId)
                ?: throw UnauthorizedException("User not found")

        val errors = mutableMapOf<String, List<String>>()

        val emailVo = email?.let { parseEmail(it, errors) }
        if (emailVo != null && emailVo != user.email && userRepository.existsByEmail(emailVo)) {
            errors["email"] = listOf("is already taken")
        }

        val usernameVo = username?.let { parseUsername(it, errors) }
        if (usernameVo != null && usernameVo != user.username && userRepository.existsByUsername(usernameVo)) {
            errors["username"] = listOf("is already taken")
        }

        password?.let {
            if (it.length < MIN_PASSWORD_LENGTH) {
                errors["password"] = listOf("must be at least $MIN_PASSWORD_LENGTH characters")
            }
        }

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }

        val now = clock.now()
        var updatedUser = user.updateProfile(now, emailVo, usernameVo, bio, image)

        password?.let {
            updatedUser = updatedUser.updatePassword(passwordHashing.hash(it), now)
        }

        val saved = userRepository.update(updatedUser)
        return saved.id.value
    }

    private fun parseEmail(
        value: String,
        errors: MutableMap<String, List<String>>,
    ): Email? {
        if (value.isBlank()) {
            errors["email"] = listOf("must not be blank")
            return null
        }
        return runCatching { Email(value) }
            .onFailure { errors["email"] = listOf("must be a valid email address") }
            .getOrNull()
    }

    private fun parseUsername(
        value: String,
        errors: MutableMap<String, List<String>>,
    ): Username? {
        if (value.isBlank()) {
            errors["username"] = listOf("must not be blank")
            return null
        }
        return runCatching { Username(value) }
            .onFailure {
                errors["username"] =
                    listOf(
                        "must be between ${Username.MIN_LENGTH} and ${Username.MAX_LENGTH} characters",
                    )
            }.getOrNull()
    }
}
