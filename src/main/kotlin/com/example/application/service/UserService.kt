package com.example.application.service

import com.example.application.port.Clock
import com.example.application.port.UserRepository
import com.example.application.port.security.PasswordHashing
import com.example.application.port.security.TokenIssuer
import com.example.application.readmodel.AuthenticatedUser
import com.example.application.readmodel.UserReadModel
import com.example.application.usecase.UserCommands
import com.example.application.usecase.UserQueries
import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.PasswordHash
import com.example.domain.aggregate.user.User
import com.example.domain.aggregate.user.UserId
import com.example.domain.aggregate.user.Username
import com.example.domain.exception.NotFoundException
import com.example.domain.exception.UnauthorizedException
import com.example.domain.exception.ValidationException
import io.micrometer.core.annotation.Counted
import io.micrometer.core.annotation.Timed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import java.util.UUID

@ApplicationScoped
class UserService(
    private val userRepository: UserRepository,
    private val passwordHashing: PasswordHashing,
    private val clock: Clock,
    private val tokenIssuer: TokenIssuer,
) : UserCommands,
    UserQueries {
    @Timed("user.registration")
    @Counted("user.registration.count")
    @Transactional
    override fun register(
        email: String,
        username: String,
        password: String,
    ): AuthenticatedUser {
        val errors = mutableMapOf<String, List<String>>()

        val emailVo = parseEmail(email, errors)
        if (emailVo != null && userRepository.existsByEmail(emailVo)) {
            errors["email"] = listOf("is already taken")
        }

        val usernameVo = parseUsername(username, errors)
        if (usernameVo != null && userRepository.existsByUsername(usernameVo)) {
            errors["username"] = listOf("is already taken")
        }

        validatePassword(password, errors)

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
        val tokens = tokenIssuer.issueTokens(userId)
        val readModel = userRepository.findReadModelById(userId) ?: throw NotFoundException("User not found")
        return AuthenticatedUser(user = readModel, accessToken = tokens.accessToken, refreshToken = tokens.refreshToken)
    }

    @Timed("user.login")
    @Transactional
    override fun login(
        email: String,
        password: String,
    ): AuthenticatedUser {
        val emailVo = runCatching { Email(email) }.getOrNull()
        val credentials = emailVo?.let { userRepository.findCredentialsByEmail(it) }

        // Always run Argon2 verify, even when the email is unknown or invalid, so that
        // login latency does not leak whether an account exists for that email.
        val verified =
            if (credentials != null) {
                passwordHashing.verify(credentials.passwordHash, password)
            } else {
                passwordHashing.verify(dummyHash, password)
                false
            }

        if (!verified) {
            logger.info("Login failed: invalid credentials")
            throw UnauthorizedException("Invalid email or password")
        }

        val userId = credentials!!.userId
        val tokens = tokenIssuer.issueTokens(userId)
        val readModel = userRepository.findReadModelById(userId) ?: throw NotFoundException("User not found")
        return AuthenticatedUser(user = readModel, accessToken = tokens.accessToken, refreshToken = tokens.refreshToken)
    }

    private val dummyHash: PasswordHash = passwordHashing.hash("timing-equalizer-not-a-real-password")

    @Transactional
    override fun updateUser(
        userId: UserId,
        email: String?,
        username: String?,
        password: String?,
        bio: String?,
        image: String?,
    ): AuthenticatedUser {
        val user =
            userRepository.findById(userId)
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

        validatePassword(password, errors)

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }

        val now = clock.now()
        var updatedUser = user.updateProfile(now, emailVo, usernameVo, bio, image)

        password?.let {
            updatedUser = updatedUser.updatePassword(passwordHashing.hash(it), now)
        }

        userRepository.update(updatedUser)
        tokenIssuer.revokeAllRefreshTokens(userId)
        val tokens = tokenIssuer.issueTokens(userId)
        val readModel = userRepository.findReadModelById(userId) ?: throw NotFoundException("User not found")
        return AuthenticatedUser(user = readModel, accessToken = tokens.accessToken, refreshToken = tokens.refreshToken)
    }

    @Transactional
    override fun eraseUser(
        userId: UserId,
        jti: UUID?,
    ) {
        tokenIssuer.revokeAllRefreshTokens(userId)
        if (jti != null) tokenIssuer.revokeAccessToken(jti, userId)
        userRepository.erase(userId)
        logger.info("User erased: userId={}", userId.value)
    }

    @Transactional
    override fun refresh(refreshToken: String): AuthenticatedUser {
        val stored =
            tokenIssuer.findRefreshToken(refreshToken)
                ?: throw UnauthorizedException("Invalid refresh token")

        val now = clock.now()
        if (stored.revokedAt != null || stored.expiresAt.isBefore(now)) {
            throw UnauthorizedException("Invalid refresh token")
        }

        // false means a concurrent request already won the UPDATE race — token already used.
        if (!tokenIssuer.revokeRefreshToken(refreshToken)) {
            throw UnauthorizedException("Invalid refresh token")
        }
        // Issue new tokens in the same transaction so revoke + issue are atomic.
        // If either step fails the whole transaction rolls back, leaving the old token intact.
        val tokens = tokenIssuer.issueTokens(stored.userId)
        val readModel = userRepository.findReadModelById(stored.userId) ?: throw NotFoundException("User not found")
        return AuthenticatedUser(user = readModel, accessToken = tokens.accessToken, refreshToken = tokens.refreshToken)
    }

    @Transactional
    override fun logout(
        refreshToken: String,
        jti: UUID?,
        userId: UserId?,
    ) {
        tokenIssuer.revokeRefreshToken(refreshToken)
        if (jti != null && userId != null) tokenIssuer.revokeAccessToken(jti, userId)
    }

    override fun getUserById(id: UserId): UserReadModel? = userRepository.findReadModelById(id)

    private fun validatePassword(
        password: String?,
        errors: MutableMap<String, List<String>>,
    ) {
        if (password != null && password.length < MIN_PASSWORD_LENGTH) {
            errors["password"] = listOf("must be at least $MIN_PASSWORD_LENGTH characters")
        }
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
        private val logger = LoggerFactory.getLogger(UserService::class.java)
    }
}
