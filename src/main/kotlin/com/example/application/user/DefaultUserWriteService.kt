package com.example.application.user

import com.example.domain.auth.PasswordHashing
import com.example.domain.shared.Clock
import com.example.domain.shared.UnauthorizedException
import com.example.domain.shared.ValidationException
import com.example.domain.user.PasswordHash
import com.example.domain.user.User
import com.example.domain.user.UserId
import com.example.domain.user.UserRepository
import io.micrometer.core.annotation.Counted
import io.micrometer.core.annotation.Timed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory

@ApplicationScoped
class DefaultUserWriteService(
    private val userRepository: UserRepository,
    private val passwordHashing: PasswordHashing,
    private val clock: Clock,
) : UserWriteService {
    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private val logger = LoggerFactory.getLogger(DefaultUserWriteService::class.java)
    }

    @Timed("user.registration")
    @Counted("user.registration.count")
    @Transactional
    override fun register(
        email: String,
        username: String,
        password: String,
    ): Long {
        val errors = mutableMapOf<String, List<String>>()

        validateEmailFormat(email, errors)
        if ("email" !in errors && userRepository.existsByEmail(email)) {
            errors["email"] = listOf("is already taken")
        }

        validateUsernameFormat(username, errors)
        if ("username" !in errors && userRepository.existsByUsername(username)) {
            errors["username"] = listOf("is already taken")
        }

        if (password.length < MIN_PASSWORD_LENGTH) {
            errors["password"] = listOf("must be at least $MIN_PASSWORD_LENGTH characters")
        }

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }

        val userId = userRepository.nextId()
        val passwordHash = passwordHashing.hash(password).value
        val user = User(id = userId, email = email, username = username, passwordHash = passwordHash)
        userRepository.create(user)
        logger.info("User registered: userId={}, username={}", userId.value, username)
        return userId.value
    }

    @Timed("user.login")
    override fun login(
        email: String,
        password: String,
    ): Long {
        val user = userRepository.findByEmail(email)
        if (user == null) {
            logger.info("Login failed: invalid credentials")
            throw UnauthorizedException("Invalid email or password")
        }

        if (!passwordHashing.verify(PasswordHash(user.passwordHash), password)) {
            logger.info("Login failed: invalid credentials")
            throw UnauthorizedException("Invalid email or password")
        }

        return user.id.value
    }

    @Transactional
    override fun updateUser(
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

        email?.let {
            validateEmailFormat(it, errors)
            if ("email" !in errors && it != user.email && userRepository.existsByEmail(it)) {
                errors["email"] = listOf("is already taken")
            }
        }

        username?.let {
            validateUsernameFormat(it, errors)
            if ("username" !in errors && it != user.username && userRepository.existsByUsername(it)) {
                errors["username"] = listOf("is already taken")
            }
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
        var updatedUser = user.updateProfile(now, email, username, bio, image)

        password?.let {
            val newPasswordHash = passwordHashing.hash(it).value
            updatedUser = updatedUser.updatePassword(newPasswordHash, now)
        }

        val saved = userRepository.update(updatedUser)
        return saved.id.value
    }

    private fun validateEmailFormat(
        email: String,
        errors: MutableMap<String, List<String>>,
    ) {
        if (email.isBlank()) {
            errors["email"] = listOf("must not be blank")
        } else if (!User.EMAIL_REGEX.matches(email)) {
            errors["email"] = listOf("must be a valid email address")
        }
    }

    private fun validateUsernameFormat(
        username: String,
        errors: MutableMap<String, List<String>>,
    ) {
        if (username.isBlank()) {
            errors["username"] = listOf("must not be blank")
        } else if (username.length !in User.MIN_USERNAME_LENGTH..User.MAX_USERNAME_LENGTH) {
            errors["username"] =
                listOf(
                    "must be between ${User.MIN_USERNAME_LENGTH} and ${User.MAX_USERNAME_LENGTH} characters",
                )
        }
    }
}
