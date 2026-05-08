package com.example.application.service

import com.example.application.port.Clock
import com.example.application.port.UserFinder
import com.example.application.port.UserRepository
import com.example.application.port.security.PasswordHashing
import com.example.application.port.security.TokenIssuer
import com.example.application.readmodel.AuthenticatedUser
import com.example.application.readmodel.UserReadModel
import com.example.application.usecase.UserCommands
import com.example.application.usecase.UserQueries
import com.example.application.validation.Validation
import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.PasswordHash
import com.example.domain.aggregate.user.User
import com.example.domain.aggregate.user.UserId
import com.example.domain.aggregate.user.Username
import com.example.domain.exception.NotFoundException
import com.example.domain.exception.UnauthorizedException
import io.micrometer.core.annotation.Counted
import io.micrometer.core.annotation.Timed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import java.util.UUID

@ApplicationScoped
class UserService(
    private val userRepository: UserRepository,
    private val userFinder: UserFinder,
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
        val v = Validation()
        val emailRes = v.parse("email") { Email(email) }
        val usernameRes = v.parse("username") { Username(username) }
        v.check("password", password.length >= MIN_PASSWORD_LENGTH) {
            "must be at least $MIN_PASSWORD_LENGTH characters"
        }
        emailRes.onSuccess {
            v.check("email", !userRepository.existsByEmail(it)) { "is already taken" }
        }
        usernameRes.onSuccess {
            v.check("username", !userRepository.existsByUsername(it)) { "is already taken" }
        }
        v.throwIfInvalid()

        val userId = userRepository.nextId()
        val user =
            User(
                id = userId,
                email = emailRes.getOrThrow(),
                username = usernameRes.getOrThrow(),
                passwordHash = passwordHashing.hash(password),
            )
        userRepository.create(user)
        logger.info("User registered: userId={}, username={}", userId.value, username)
        val tokens = tokenIssuer.issueTokens(userId)
        val readModel = userFinder.findReadModelById(userId) ?: throw NotFoundException("User not found")
        return AuthenticatedUser(user = readModel, accessToken = tokens.accessToken, refreshToken = tokens.refreshToken)
    }

    @Timed("user.login")
    @Transactional
    override fun login(
        email: String,
        password: String,
    ): AuthenticatedUser {
        val emailVo = runCatching { Email(email) }.getOrNull()
        val credentials = emailVo?.let { userFinder.findCredentialsByEmail(it) }

        // Always run Argon2 verify, even when the email is unknown or invalid, so that
        // login latency does not leak whether an account exists for that email.
        val verified =
            if (credentials != null) {
                passwordHashing.verify(credentials.passwordHash, password)
            } else {
                passwordHashing.verify(dummyHash, password)
                false
            }

        if (!verified || credentials == null) {
            logger.info("Login failed: invalid credentials")
            throw UnauthorizedException("Invalid email or password")
        }

        val userId = credentials.userId
        val tokens = tokenIssuer.issueTokens(userId)
        val readModel = userFinder.findReadModelById(userId) ?: throw NotFoundException("User not found")
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

        val v = Validation()
        val emailVo = email?.let { v.parse("email") { Email(it) }.getOrNull() }
        val usernameVo = username?.let { v.parse("username") { Username(it) }.getOrNull() }
        v.check("password", password == null || password.length >= MIN_PASSWORD_LENGTH) {
            "must be at least $MIN_PASSWORD_LENGTH characters"
        }
        if (emailVo != null && emailVo != user.email) {
            v.check("email", !userRepository.existsByEmail(emailVo)) { "is already taken" }
        }
        if (usernameVo != null && usernameVo != user.username) {
            v.check("username", !userRepository.existsByUsername(usernameVo)) { "is already taken" }
        }
        v.throwIfInvalid()

        val now = clock.now()
        val updatedUser =
            user
                .updateProfile(now, emailVo, usernameVo, bio, image)
                .let { profile -> password?.let { profile.updatePassword(passwordHashing.hash(it), now) } ?: profile }

        userRepository.update(updatedUser)
        tokenIssuer.revokeAllRefreshTokens(userId)
        val tokens = tokenIssuer.issueTokens(userId)
        val readModel = userFinder.findReadModelById(userId) ?: throw NotFoundException("User not found")
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
        val readModel = userFinder.findReadModelById(stored.userId) ?: throw NotFoundException("User not found")
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

    override fun getUserById(id: UserId): UserReadModel? = userFinder.findReadModelById(id)

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private val logger = LoggerFactory.getLogger(UserService::class.java)
    }
}
