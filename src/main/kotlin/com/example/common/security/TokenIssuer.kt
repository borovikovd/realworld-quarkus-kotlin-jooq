package com.example.common.security

import com.example.user.UserId
import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.jwt.Claims
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

@ApplicationScoped
class TokenIssuer(
    @param:ConfigProperty(name = "mp.jwt.verify.issuer") private val issuer: String,
    @param:ConfigProperty(name = "app.token.access-expiry-seconds") private val accessExpirySeconds: Long,
    @param:ConfigProperty(name = "app.token.refresh-expiry-days") private val refreshExpiryDays: Long,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val revokedTokenRepository: RevokedTokenRepository,
) {
    private val secureRandom = SecureRandom()
    private val base64UrlEncoder = Base64.getUrlEncoder().withoutPadding()
    private val accessTokenExpiry: Duration = Duration.ofSeconds(accessExpirySeconds)
    private val refreshTokenExpiry: Duration = Duration.ofDays(refreshExpiryDays)

    @Transactional
    fun issue(userId: UserId): IssuedTokens = mintPair(userId, UUID.randomUUID())

    /**
     * On reuse, revokes only the affected family (RFC 9700 §4.14.2) so sibling sessions
     * on other devices keep working.
     */
    @Transactional
    fun refresh(rawRefreshToken: String): RefreshResult? {
        val hash = sha256(rawRefreshToken)
        val stored = refreshTokenRepository.findByHash(hash)

        return when {
            stored == null -> {
                logger.info("Refresh failed: token not found")
                null
            }
            stored.expiresAt.isBefore(OffsetDateTime.now()) -> {
                logger.info("Refresh failed: token expired for userId={}", stored.userId.value)
                null
            }
            stored.revokedAt != null -> reuseDetected(stored, "replayed after revocation")
            !refreshTokenRepository.revokeByHash(hash) -> reuseDetected(stored, "lost concurrent rotation race")
            else -> RefreshResult(userId = stored.userId, tokens = mintPair(stored.userId, stored.familyId))
        }
    }

    private fun reuseDetected(
        stored: StoredRefreshToken,
        reason: String,
    ): RefreshResult? {
        logger.warn(
            "Refresh token reuse ({}), revoking family={} for userId={}",
            reason,
            stored.familyId,
            stored.userId.value,
        )
        refreshTokenRepository.revokeFamily(stored.familyId)
        return null
    }

    /** The [userId] predicate prevents A from revoking B's refresh token by submitting it. */
    @Transactional
    fun revokeSession(
        rawRefreshToken: String,
        userId: UserId,
        jti: UUID?,
    ) {
        refreshTokenRepository.revokeByHashAndUser(sha256(rawRefreshToken), userId)
        if (jti != null) blocklistAccessToken(jti)
    }

    /**
     * Access tokens on other devices stay valid until natural expiry — short TTL is the
     * bound. The refresh-token sweep prevents them from being renewed.
     */
    @Transactional
    fun revokeAllSessions(
        userId: UserId,
        currentJti: UUID?,
    ) {
        refreshTokenRepository.revokeAllForUser(userId)
        if (currentJti != null) blocklistAccessToken(currentJti)
    }

    private fun mintPair(
        userId: UserId,
        familyId: UUID,
    ): IssuedTokens {
        val accessToken = generateAccessToken(userId)
        val refreshToken = generateRefreshToken()
        refreshTokenRepository.store(
            userId = userId,
            familyId = familyId,
            tokenHash = sha256(refreshToken),
            expiresAt = OffsetDateTime.now().plus(refreshTokenExpiry),
        )
        return IssuedTokens(accessToken = accessToken, refreshToken = refreshToken)
    }

    /** For when refresh-token revocation is handled elsewhere (e.g. cascade on user delete). */
    @Transactional
    fun revokeAccessToken(jti: UUID) = blocklistAccessToken(jti)

    private fun blocklistAccessToken(jti: UUID) =
        revokedTokenRepository.insert(jti, OffsetDateTime.now().plus(accessTokenExpiry))

    private fun generateAccessToken(userId: UserId): String =
        Jwt
            .issuer(issuer)
            .subject(userId.value.toString())
            .groups(setOf("user"))
            .claim(Claims.jti, UUID.randomUUID().toString())
            .expiresAt(OffsetDateTime.now().toInstant().plus(accessTokenExpiry))
            .sign()

    private fun generateRefreshToken(): String {
        val bytes = ByteArray(REFRESH_TOKEN_BYTES).also { secureRandom.nextBytes(it) }
        return base64UrlEncoder.encodeToString(bytes)
    }

    companion object {
        private const val REFRESH_TOKEN_BYTES = 32
        private val logger = LoggerFactory.getLogger(TokenIssuer::class.java)

        private fun sha256(value: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            return Base64.getEncoder().encodeToString(digest)
        }
    }
}
