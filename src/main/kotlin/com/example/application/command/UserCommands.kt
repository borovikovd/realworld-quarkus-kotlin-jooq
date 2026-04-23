package com.example.application.command

import com.example.application.port.inbound.command.LoginUserCommand
import com.example.application.port.inbound.command.RegisterUserCommand
import com.example.application.port.inbound.command.UpdateUserCommand
import com.example.application.port.outbound.Clock
import com.example.application.port.outbound.PasswordHashing
import com.example.application.port.outbound.UserWriteRepository
import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.User
import com.example.domain.aggregate.user.UserId
import com.example.domain.aggregate.user.Username
import com.example.domain.exception.UnauthorizedException
import com.example.domain.exception.ValidationException
import io.micrometer.core.annotation.Counted
import io.micrometer.core.annotation.Timed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory

@ApplicationScoped
class UserCommands(
    private val userWriteRepository: UserWriteRepository,
    private val passwordHashing: PasswordHashing,
    private val clock: Clock,
) {
    @Timed("user.registration")
    @Counted("user.registration.count")
    @Transactional
    fun register(command: RegisterUserCommand): Long {
        val errors = mutableMapOf<String, List<String>>()

        val emailVo = parseEmail(command.email, errors)
        if (emailVo != null && userWriteRepository.existsByEmail(emailVo)) {
            errors["email"] = listOf("is already taken")
        }

        val usernameVo = parseUsername(command.username, errors)
        if (usernameVo != null && userWriteRepository.existsByUsername(usernameVo)) {
            errors["username"] = listOf("is already taken")
        }

        if (command.password.length < MIN_PASSWORD_LENGTH) {
            errors["password"] = listOf("must be at least $MIN_PASSWORD_LENGTH characters")
        }

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }

        val userId = userWriteRepository.nextId()
        val user =
            User(
                id = userId,
                email = emailVo!!,
                username = usernameVo!!,
                passwordHash = passwordHashing.hash(command.password),
            )
        userWriteRepository.create(user)
        logger.info("User registered: userId={}, username={}", userId.value, command.username)
        return userId.value
    }

    @Timed("user.login")
    fun login(command: LoginUserCommand): Long {
        val emailVo =
            runCatching { Email(command.email) }
                .getOrElse { throw UnauthorizedException("Invalid email or password") }

        val user = userWriteRepository.findByEmail(emailVo)
        if (user == null) {
            logger.info("Login failed: invalid credentials")
            throw UnauthorizedException("Invalid email or password")
        }

        if (!passwordHashing.verify(user.passwordHash, command.password)) {
            logger.info("Login failed: invalid credentials")
            throw UnauthorizedException("Invalid email or password")
        }

        return user.id.value
    }

    @Transactional
    fun updateUser(command: UpdateUserCommand): Long {
        val typedUserId = UserId(command.userId)
        val user =
            userWriteRepository.findById(typedUserId)
                ?: throw UnauthorizedException("User not found")

        val errors = mutableMapOf<String, List<String>>()

        val emailVo = command.email?.let { parseEmail(it, errors) }
        if (emailVo != null && emailVo != user.email && userWriteRepository.existsByEmail(emailVo)) {
            errors["email"] = listOf("is already taken")
        }

        val usernameVo = command.username?.let { parseUsername(it, errors) }
        if (usernameVo != null && usernameVo != user.username && userWriteRepository.existsByUsername(usernameVo)) {
            errors["username"] = listOf("is already taken")
        }

        command.password?.let {
            if (it.length < MIN_PASSWORD_LENGTH) {
                errors["password"] = listOf("must be at least $MIN_PASSWORD_LENGTH characters")
            }
        }

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }

        val now = clock.now()
        var updatedUser = user.updateProfile(now, emailVo, usernameVo, command.bio, command.image)

        command.password?.let {
            updatedUser = updatedUser.updatePassword(passwordHashing.hash(it), now)
        }

        val saved = userWriteRepository.update(updatedUser)
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
                    listOf("must be between ${Username.MIN_LENGTH} and ${Username.MAX_LENGTH} characters")
            }.getOrNull()
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private val logger = LoggerFactory.getLogger(UserCommands::class.java)
    }
}
