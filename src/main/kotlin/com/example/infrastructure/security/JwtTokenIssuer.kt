package com.example.infrastructure.security

import com.example.application.port.Clock
import com.example.application.port.security.CryptoService
import com.example.application.port.security.RefreshTokenRepository
import com.example.application.port.security.RevokedTokenRepository
import com.example.application.port.security.TokenIssuer
import com.example.application.readmodel.IssuedTokens
import com.example.application.readmodel.StoredRefreshToken
import com.example.domain.aggregate.user.UserId
import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.security.SecureRandom
import java.time.Duration
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

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

    override fun accessTokenExpiry(): Duration = accessTokenExpiry

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
        userId: Long,
    ) = revokedTokenRepository.insert(jti, userId, clock.now().plus(accessTokenExpiry))

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
