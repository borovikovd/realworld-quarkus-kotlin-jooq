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

    /** Mints a fresh access+refresh pair under a brand-new family (login, register, post-credential-change). */
    @Transactional
    fun issue(userId: UserId): IssuedTokens = mintPair(userId, UUID.randomUUID())

    /**
     * Exchanges a refresh token for a fresh access+refresh pair, rotating the refresh token
     * while preserving its family. Returns null for any invalid outcome (not found, expired,
     * already revoked).
     *
     * On reuse (presenting a previously-revoked token from this chain), only the affected
     * family is revoked — sibling sessions on other devices keep working. This is the
     * RFC 9700 §4.14.2 implementation note: "the grant to which a refresh token belongs
     * may be encoded into the refresh token itself ... by extension, all refresh tokens
     * that need to be revoked."
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

    /**
     * Terminates a single session: revokes the presented refresh token (only if it belongs
     * to [userId]) and blocklists the access token's [jti] if present. The ownership
     * predicate prevents a holder of access token A from revoking another user's refresh
     * token by submitting it.
     */
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
     * Terminates every session for [userId]: revokes all refresh tokens and, if the caller
     * is signed in, blocklists their access token's [currentJti]. Used on password/email
     * change, account erase, and refresh-token reuse detection.
     *
     * Note: access tokens for other devices remain valid until their natural expiry
     * (short-lived by design). The refresh-token sweep prevents them from being renewed.
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

        fun sha256(value: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            return Base64.getEncoder().encodeToString(digest)
        }
    }
}
