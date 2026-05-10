package com.example.user

import com.example.common.security.CurrentUser
import com.example.common.security.PasswordHash
import com.example.common.security.PasswordHashing
import com.example.common.security.TokenIssuer
import com.example.common.web.ConflictException
import com.example.common.web.InvalidCredentialsException
import com.example.common.web.NotFoundException
import com.example.common.web.Patch
import com.example.common.web.UnauthorizedException
import com.example.common.web.Validation
import com.example.common.web.ValidationException
import com.example.common.web.orElseNullable
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

@ApplicationScoped
class UserService(
    private val userRepository: UserRepository,
    private val passwordHashing: PasswordHashing,
    private val tokenIssuer: TokenIssuer,
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
        val now = OffsetDateTime.now()
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

    fun getCurrentUser(): AuthenticatedUser {
        val userId = currentUser.require()
        val rawToken = currentUser.rawToken ?: throw UnauthorizedException("Token not present")
        val user = userRepository.findById(userId) ?: throw NotFoundException("user", "User not found")
        return toAuthenticatedUser(user, rawToken, "")
    }

    @Transactional
    fun updateUser(
        email: Patch<String>,
        username: Patch<String>,
        password: Patch<String>,
        bio: Patch<String>,
        image: Patch<String>,
    ): AuthenticatedUser {
        val userId = currentUser.require()
        val user = userRepository.findById(userId) ?: throw UnauthorizedException("User not found")

        val v = Validation()
        val resolvedPassword = resolvePassword(password, v)
        val resolvedEmail = resolveEmail(email, user.email, v)
        val resolvedUsername = resolveUsername(username, user.username, v)
        if (bio is Patch.Present && bio.value != null) {
            v.check("bio", bio.value.length <= MAX_BIO_LENGTH) { "must be at most $MAX_BIO_LENGTH characters" }
        }
        if (image is Patch.Present && image.value != null) {
            v.check("image", image.value.length <= MAX_IMAGE_LENGTH) { "must be at most $MAX_IMAGE_LENGTH characters" }
        }
        v.throwIfInvalid()

        val now = OffsetDateTime.now()
        val updated =
            user.copy(
                email = resolvedEmail,
                username = resolvedUsername,
                passwordHash =
                    if (resolvedPassword != null) passwordHashing.hash(resolvedPassword) else user.passwordHash,
                bio = bio.orElseNullable(user.bio)?.ifBlank { null },
                image = image.orElseNullable(user.image)?.ifBlank { null },
                updatedAt = now,
            )

        userRepository.update(updated)
        tokenIssuer.revokeAllRefreshTokens(userId)
        val jti = currentUser.jti
        if (jti != null) tokenIssuer.revokeAccessToken(jti, userId)
        val tokens = tokenIssuer.issueTokens(userId)
        return toAuthenticatedUser(updated, tokens.accessToken, tokens.refreshToken)
    }

    @Transactional
    fun eraseUser() {
        val userId = currentUser.require()
        val jti = currentUser.jti
        tokenIssuer.revokeAllRefreshTokens(userId)
        if (jti != null) tokenIssuer.revokeAccessToken(jti, userId)
        userRepository.erase(userId)
        logger.info("User erased: userId={}", userId.value)
    }

    /** Returns null on any invalid-token outcome; the resource layer translates that to 401. */
    @Transactional
    fun refresh(refreshToken: String): AuthenticatedUser? {
        val stored = tokenIssuer.findRefreshToken(refreshToken)

        return when {
            stored == null -> null
            stored.expiresAt.isBefore(OffsetDateTime.now()) -> null
            stored.revokedAt != null || !tokenIssuer.revokeRefreshToken(refreshToken) -> {
                tokenIssuer.revokeAllRefreshTokens(stored.userId)
                null
            }
            else -> issueRotatedTokens(stored.userId)
        }
    }

    private fun issueRotatedTokens(userId: UserId): AuthenticatedUser {
        val tokens = tokenIssuer.issueTokens(userId)
        val user = userRepository.findById(userId) ?: throw NotFoundException("user", "User not found")
        return toAuthenticatedUser(user, tokens.accessToken, tokens.refreshToken)
    }

    @Transactional
    fun logout(refreshToken: String) {
        val userId = currentUser.id
        val jti = currentUser.jti
        // Only revoke the refresh token if it belongs to the authenticated user, so a holder
        // of token X can't revoke another user's refresh token by submitting it in the body.
        val stored = tokenIssuer.findRefreshToken(refreshToken)
        if (stored != null && stored.userId == userId) {
            tokenIssuer.revokeRefreshToken(refreshToken)
        }
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

    private fun resolvePassword(
        patch: Patch<String>,
        v: Validation,
    ): String? =
        when (patch) {
            is Patch.Absent -> null
            is Patch.Present -> {
                val value = patch.value
                v.check("password", !value.isNullOrBlank()) { "can't be blank" }
                v.check("password", value == null || value.length >= MIN_PASSWORD_LENGTH) {
                    "must be at least $MIN_PASSWORD_LENGTH characters"
                }
                value
            }
        }

    private fun resolveEmail(
        patch: Patch<String>,
        current: String,
        v: Validation,
    ): String =
        when (patch) {
            is Patch.Absent -> current
            is Patch.Present -> {
                val value = patch.value
                v.check("email", !value.isNullOrBlank()) { "can't be blank" }
                if (value != null && value != current) {
                    v.check("email", !userRepository.existsByEmail(value)) { "has already been taken" }
                }
                value ?: current
            }
        }

    private fun resolveUsername(
        patch: Patch<String>,
        current: String,
        v: Validation,
    ): String =
        when (patch) {
            is Patch.Absent -> current
            is Patch.Present -> {
                val value = patch.value
                v.check("username", !value.isNullOrBlank()) { "can't be blank" }
                if (value != null && value != current) {
                    v.check("username", !userRepository.existsByUsername(value)) { "has already been taken" }
                }
                value ?: current
            }
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
        private const val MAX_BIO_LENGTH = 4096
        private const val MAX_IMAGE_LENGTH = 1024
        private val logger = LoggerFactory.getLogger(UserService::class.java)
    }
}
