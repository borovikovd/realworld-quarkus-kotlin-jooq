package com.example.application.service

import com.example.application.inport.command.UserCommands
import com.example.application.inport.query.UserQueries
import com.example.application.outport.Clock
import com.example.application.outport.CryptoService
import com.example.application.outport.PasswordHashing
import com.example.application.outport.RefreshTokenRepository
import com.example.application.outport.RevokedTokenRepository
import com.example.application.outport.TokenIssuer
import com.example.application.outport.UserRepository
import com.example.application.readmodel.RefreshedSession
import com.example.application.readmodel.UserReadModel
import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.PasswordHash
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
    override fun login(
        email: String,
        password: String,
    ): Long {
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

        return credentials!!.userId.value
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
            refreshTokenRepository.revokeAllForUser(typedUserId, now)
            updatedUser = updatedUser.updatePassword(passwordHashing.hash(it), now)
        }

        val saved = userRepository.update(updatedUser)
        return saved.id.value
    }

    @Transactional
    override fun eraseUser(userId: Long) {
        val typedUserId = UserId(userId)
        refreshTokenRepository.revokeAllForUser(typedUserId, clock.now())
        userRepository.erase(typedUserId)
        logger.info("User erased: userId={}", userId)
    }

    @Transactional
    override fun refresh(refreshToken: String): RefreshedSession {
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
        return RefreshedSession(userId = stored.userId.value, tokens = tokens)
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
            val expiry = clock.now().plusMinutes(ACCESS_TOKEN_EXPIRY_MINUTES)
            revokedTokenRepository.insert(jti, userId, expiry)
        }
    }

    override fun getUserById(id: Long): UserReadModel? = userRepository.findById(id)

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
        private const val ACCESS_TOKEN_EXPIRY_MINUTES = 16L
        private val logger = LoggerFactory.getLogger(UserApplicationService::class.java)
    }
}
