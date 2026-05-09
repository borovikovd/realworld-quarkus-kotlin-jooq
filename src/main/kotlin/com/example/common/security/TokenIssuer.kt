package com.example.common.security

import com.example.common.time.Clock
import com.example.user.UserId
import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.security.SecureRandom
import java.time.Duration
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

interface TokenIssuer {
    fun issueTokens(userId: UserId): IssuedTokens

    fun findRefreshToken(token: String): StoredRefreshToken?

    /** Returns false if the token was already revoked or not found. */
    fun revokeRefreshToken(token: String): Boolean

    fun revokeAllRefreshTokens(userId: UserId)

    fun revokeAccessToken(
        jti: UUID,
        userId: UserId,
    )

    fun purgeExpiredRefreshTokens(before: OffsetDateTime): Int

    fun purgeExpiredAccessTokenRevocations(before: OffsetDateTime): Int
}

@ApplicationScoped
class JwtTokenIssuer(
    @param:ConfigProperty(name = "mp.jwt.verify.issuer") private val issuer: String,
    @param:ConfigProperty(name = "app.token.access-expiry-seconds") private val accessExpirySeconds: Long,
    @param:ConfigProperty(name = "app.token.refresh-expiry-days") private val refreshExpiryDays: Long,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val revokedTokenRepository: RevokedTokenRepository,
    private val crypto: CryptoService,
    private val clock: Clock,
) : TokenIssuer {
    private val secureRandom = SecureRandom()
    private val base64UrlEncoder = Base64.getUrlEncoder().withoutPadding()
    private val accessTokenExpiry: Duration = Duration.ofSeconds(accessExpirySeconds)
    private val refreshTokenExpiry: Duration = Duration.ofDays(refreshExpiryDays)

    override fun issueTokens(userId: UserId): IssuedTokens {
        val accessToken = generateAccessToken(userId)
        val refreshToken = generateRefreshToken()
        refreshTokenRepository.store(
            userId = userId,
            tokenHash = crypto.hmacRefreshToken(refreshToken),
            expiresAt = clock.now().plus(refreshTokenExpiry),
        )
        return IssuedTokens(accessToken = accessToken, refreshToken = refreshToken)
    }

    override fun findRefreshToken(token: String): StoredRefreshToken? =
        refreshTokenRepository.findByHash(crypto.hmacRefreshToken(token))

    override fun revokeRefreshToken(token: String): Boolean =
        refreshTokenRepository.revokeByHash(crypto.hmacRefreshToken(token), clock.now())

    override fun revokeAllRefreshTokens(userId: UserId) = refreshTokenRepository.revokeAllForUser(userId, clock.now())

    override fun revokeAccessToken(
        jti: UUID,
        userId: UserId,
    ) = revokedTokenRepository.insert(jti, userId.value, clock.now().plus(accessTokenExpiry))

    override fun purgeExpiredRefreshTokens(before: OffsetDateTime): Int =
        refreshTokenRepository.deleteExpiredBefore(before)

    override fun purgeExpiredAccessTokenRevocations(before: OffsetDateTime): Int =
        revokedTokenRepository.deleteExpiredBefore(before)

    private fun generateAccessToken(userId: UserId): String =
        Jwt
            .issuer(issuer)
            .subject(userId.value.toString())
            .groups(setOf("user"))
            .expiresAt(clock.now().toInstant().plus(accessTokenExpiry))
            .sign()

    private fun generateRefreshToken(): String {
        val bytes = ByteArray(REFRESH_TOKEN_BYTES).also { secureRandom.nextBytes(it) }
        return base64UrlEncoder.encodeToString(bytes)
    }

    companion object {
        private const val REFRESH_TOKEN_BYTES = 32
    }
}
