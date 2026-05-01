package com.example.application.service

import com.example.application.port.Clock
import com.example.application.port.UserRepository
import com.example.application.port.security.CryptoService
import com.example.application.port.security.PasswordHashing
import com.example.application.port.security.RefreshTokenRepository
import com.example.application.port.security.RevokedTokenRepository
import com.example.application.port.security.TokenIssuer
import com.example.application.readmodel.AuthenticatedUser
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
class UserApplicationService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val revokedTokenRepository: RevokedTokenRepository,
    private val passwordHashing: PasswordHashing,
    private val crypto: CryptoService,
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
        val tokens = tokenIssuer.issue(userId)
        val readModel = userRepository.findById(userId.value) ?: throw NotFoundException("User not found")
        return AuthenticatedUser(user = readModel, accessToken = tokens.accessToken, refreshToken = tokens.refreshToken)
    }

    @Timed("user.login")
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
        val tokens = tokenIssuer.issue(userId)
        val readModel = userRepository.findById(userId.value) ?: throw NotFoundException("User not found")
        return AuthenticatedUser(user = readModel, accessToken = tokens.accessToken, refreshToken = tokens.refreshToken)
    }

    private val dummyHash: PasswordHash = passwordHashing.hash("timing-equalizer-not-a-real-password")

    @Transactional
    override fun updateUser(
        userId: Long,
        email: String?,
        username: String?,
        password: String?,
        bio: String?,
        image: String?,
    ): AuthenticatedUser {
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

        validatePassword(password, errors)

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }

        val now = clock.now()
        var updatedUser = user.updateProfile(now, emailVo, usernameVo, bio, image)

        password?.let {
            refreshTokenRepository.revokeAllForUser(typedUserId, now)
            updatedUser = updatedUser.updatePassword(passwordHashing.hash(it), now)
        }

        userRepository.update(updatedUser)
        val readModel = userRepository.findById(userId) ?: throw NotFoundException("User not found")
        return AuthenticatedUser(
            user = readModel,
            accessToken = tokenIssuer.issueAccessToken(typedUserId),
            refreshToken = "",
        )
    }

    @Transactional
    override fun eraseUser(userId: Long) {
        val typedUserId = UserId(userId)
        refreshTokenRepository.revokeAllForUser(typedUserId, clock.now())
        userRepository.erase(typedUserId)
        logger.info("User erased: userId={}", userId)
    }

    @Transactional
    override fun refresh(refreshToken: String): AuthenticatedUser {
        val tokenHash = crypto.hmacRefreshToken(refreshToken)
        val stored =
            refreshTokenRepository.findByHash(tokenHash)
                ?: throw UnauthorizedException("Invalid refresh token")

        val now = clock.now()
        if (stored.revokedAt != null || stored.expiresAt.isBefore(now)) {
            throw UnauthorizedException("Invalid refresh token")
        }

        // false means a concurrent request already won the UPDATE race — token already used.
        if (!refreshTokenRepository.revokeByHash(tokenHash, now)) {
            throw UnauthorizedException("Invalid refresh token")
        }
        // Issue new tokens in the same transaction so revoke + issue are atomic.
        // If either step fails the whole transaction rolls back, leaving the old token intact.
        val tokens = tokenIssuer.issue(stored.userId)
        val readModel = userRepository.findById(stored.userId.value) ?: throw NotFoundException("User not found")
        return AuthenticatedUser(user = readModel, accessToken = tokens.accessToken, refreshToken = tokens.refreshToken)
    }

    @Transactional
    override fun logout(
        refreshToken: String,
        jti: UUID?,
        userId: Long?,
    ) {
        val tokenHash = crypto.hmacRefreshToken(refreshToken)
        refreshTokenRepository.revokeByHash(tokenHash, clock.now())
        if (jti != null && userId != null) {
            val expiry = clock.now().plus(tokenIssuer.accessTokenExpiry())
            revokedTokenRepository.insert(jti, userId, expiry)
        }
    }

    override fun getUserById(id: Long): AuthenticatedUser? {
        val readModel = userRepository.findById(id) ?: return null
        return AuthenticatedUser(
            user = readModel,
            accessToken = tokenIssuer.issueAccessToken(UserId(id)),
            refreshToken = "",
        )
    }

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
        private val logger = LoggerFactory.getLogger(UserApplicationService::class.java)
    }
}
