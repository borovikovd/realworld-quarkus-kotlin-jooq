package com.example.user

import com.example.common.security.CurrentUser
import com.example.common.security.PasswordHash
import com.example.common.security.PasswordHashing
import com.example.common.security.TokenIssuer
import com.example.common.time.Clock
import com.example.common.web.ConflictException
import com.example.common.web.InvalidCredentialsException
import com.example.common.web.NotFoundException
import com.example.common.web.Patch
import com.example.common.web.UnauthorizedException
import com.example.common.web.Validation
import com.example.common.web.ValidationException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import java.util.UUID

@ApplicationScoped
class UserService(
    private val userRepository: UserRepository,
    private val passwordHashing: PasswordHashing,
    private val tokenIssuer: TokenIssuer,
    private val clock: Clock,
    private val currentUser: CurrentUser,
) {
    @Transactional
    fun register(
        email: String,
        username: String,
        password: String,
    ): AuthenticatedUser {
        val v = Validation()
        v.check("password", password.length >= MIN_PASSWORD_LENGTH) {
            "must be at least $MIN_PASSWORD_LENGTH characters"
        }
        v.throwIfInvalid()

        val conflicts = mutableMapOf<String, List<String>>()
        if (userRepository.existsByEmail(email)) conflicts["email"] = listOf("has already been taken")
        if (userRepository.existsByUsername(username)) conflicts["username"] = listOf("has already been taken")
        if (conflicts.isNotEmpty()) throw ConflictException(conflicts)

        val userId = userRepository.nextId()
        val now = clock.now()
        val user =
            User(
                id = userId,
                email = email,
                username = username,
                passwordHash = passwordHashing.hash(password),
                createdAt = now,
                updatedAt = now,
            )
        userRepository.insert(user)
        logger.info("User registered: userId={}, username={}", userId.value, username)

        val tokens = tokenIssuer.issueTokens(userId)
        return AuthenticatedUser(
            email = user.email,
            username = user.username,
            bio = user.bio,
            image = user.image,
            token = tokens.accessToken,
            refreshToken = tokens.refreshToken,
        )
    }

    @Transactional
    fun login(
        email: String,
        password: String,
    ): AuthenticatedUser {
        val found = userRepository.findByEmail(email)

        // Always run Argon2 verify, even when the email is unknown, so that
        // login latency does not leak whether an account exists for that email.
        val verified =
            if (found != null) {
                passwordHashing.verify(found.passwordHash, password)
            } else {
                passwordHashing.verify(dummyHash, password)
                false
            }

        if (!verified || found == null) {
            logger.info("Login failed: invalid credentials")
            throw InvalidCredentialsException()
        }

        val tokens = tokenIssuer.issueTokens(found.id)
        return toAuthenticatedUser(found, tokens.accessToken, tokens.refreshToken)
    }

    fun getCurrentUser(
        userId: UserId,
        rawToken: String,
    ): AuthenticatedUser {
        val user = userRepository.findById(userId) ?: throw NotFoundException("user", "User not found")
        return toAuthenticatedUser(user, rawToken, "")
    }

    @Transactional
    fun updateUser(
        userId: UserId,
        email: Patch<String>,
        username: Patch<String>,
        password: String?,
        bio: Patch<String>,
        image: Patch<String>,
    ): AuthenticatedUser {
        val user = userRepository.findById(userId) ?: throw UnauthorizedException("User not found")

        val v = Validation()
        v.check("password", password == null || password.length >= MIN_PASSWORD_LENGTH) {
            "must be at least $MIN_PASSWORD_LENGTH characters"
        }
        val resolvedEmail = resolveEmail(email, user.email, v)
        val resolvedUsername = resolveUsername(username, user.username, v)
        v.throwIfInvalid()

        val now = clock.now()
        val updated =
            user.copy(
                email = resolvedEmail,
                username = resolvedUsername,
                passwordHash = if (password != null) passwordHashing.hash(password) else user.passwordHash,
                bio =
                    when (bio) {
                        is Patch.Absent -> user.bio
                        is Patch.Present -> bio.value?.ifBlank { null }
                    },
                image =
                    when (image) {
                        is Patch.Absent -> user.image
                        is Patch.Present -> image.value?.ifBlank { null }
                    },
                updatedAt = now,
            )

        userRepository.update(updated)
        tokenIssuer.revokeAllRefreshTokens(userId)
        val tokens = tokenIssuer.issueTokens(userId)
        return toAuthenticatedUser(updated, tokens.accessToken, tokens.refreshToken)
    }

    @Transactional
    fun eraseUser(
        userId: UserId,
        jti: UUID?,
    ) {
        tokenIssuer.revokeAllRefreshTokens(userId)
        if (jti != null) tokenIssuer.revokeAccessToken(jti, userId)
        userRepository.erase(userId)
        logger.info("User erased: userId={}", userId.value)
    }

    @Transactional
    fun refresh(refreshToken: String): AuthenticatedUser {
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

        val tokens = tokenIssuer.issueTokens(stored.userId)
        val user = userRepository.findById(stored.userId) ?: throw NotFoundException("user", "User not found")
        return toAuthenticatedUser(user, tokens.accessToken, tokens.refreshToken)
    }

    @Transactional
    fun logout(
        refreshToken: String,
        jti: UUID?,
        userId: UserId?,
    ) {
        tokenIssuer.revokeRefreshToken(refreshToken)
        if (jti != null && userId != null) tokenIssuer.revokeAccessToken(jti, userId)
    }

    @Transactional
    fun followUser(username: String) {
        val followerId = currentUser.require()
        val followeeId =
            userRepository.findUserIdByUsername(username)
                ?: throw NotFoundException("profile", "User not found")
        if (followeeId == followerId) {
            throw ValidationException(mapOf("username" to listOf("cannot follow yourself")))
        }
        userRepository.follow(followerId, followeeId)
    }

    @Transactional
    fun unfollowUser(username: String) {
        val followerId = currentUser.require()
        val followeeId =
            userRepository.findUserIdByUsername(username)
                ?: throw NotFoundException("profile", "User not found")
        userRepository.unfollow(followerId, followeeId)
    }

    fun getProfileByUsername(
        username: String,
        viewerId: UserId?,
    ): ProfileDto? = userRepository.findProfile(username, viewerId)

    private fun resolveEmail(
        patch: Patch<String>,
        current: String,
        v: Validation,
    ): String {
        if (patch is Patch.Absent) return current
        val value = (patch as Patch.Present).value
        v.check("email", !value.isNullOrBlank()) { "can't be blank" }
        if (value != null && value != current) {
            v.check("email", !userRepository.existsByEmail(value)) { "has already been taken" }
        }
        return value ?: current
    }

    private fun resolveUsername(
        patch: Patch<String>,
        current: String,
        v: Validation,
    ): String {
        if (patch is Patch.Absent) return current
        val value = (patch as Patch.Present).value
        v.check("username", !value.isNullOrBlank()) { "can't be blank" }
        if (value != null && value != current) {
            v.check("username", !userRepository.existsByUsername(value)) { "has already been taken" }
        }
        return value ?: current
    }

    private val dummyHash: PasswordHash = passwordHashing.hash("timing-equalizer-not-a-real-password")

    private fun toAuthenticatedUser(
        user: User,
        accessToken: String,
        refreshToken: String,
    ) = AuthenticatedUser(
        email = user.email,
        username = user.username,
        bio = user.bio,
        image = user.image,
        token = accessToken,
        refreshToken = refreshToken,
    )

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private val logger = LoggerFactory.getLogger(UserService::class.java)
    }
}
