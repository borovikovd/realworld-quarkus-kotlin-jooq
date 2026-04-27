package com.example.infrastructure.security

import com.example.application.outport.CryptoService
import com.example.application.outport.RefreshTokenRepository
import com.example.application.outport.TokenIssuer
import com.example.application.readmodel.IssuedTokens
import com.example.domain.aggregate.user.UserId
import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Base64

@ApplicationScoped
class JwtTokenIssuer(
    @param:ConfigProperty(name = "mp.jwt.verify.issuer") private val issuer: String,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val crypto: CryptoService,
) : TokenIssuer {
    private val secureRandom = SecureRandom()
    private val base64UrlEncoder = Base64.getUrlEncoder().withoutPadding()

    override fun issueAccessToken(userId: UserId): String =
        Jwt
            .issuer(issuer)
            .subject(userId.value.toString())
            .groups(setOf("user"))
            .expiresAt(Instant.now().plus(ACCESS_TOKEN_EXPIRY))
            .sign()

    override fun issue(userId: UserId): IssuedTokens {
        val accessToken = issueAccessToken(userId)
        val refreshToken = generateRefreshToken()
        refreshTokenRepository.store(
            userId = userId,
            tokenHash = crypto.hmacRefreshToken(refreshToken),
            expiresAt = OffsetDateTime.now().plus(REFRESH_TOKEN_EXPIRY),
        )

        return IssuedTokens(accessToken = accessToken, refreshToken = refreshToken)
    }

    private fun generateRefreshToken(): String {
        val bytes = ByteArray(REFRESH_TOKEN_BYTES).also { secureRandom.nextBytes(it) }
        return base64UrlEncoder.encodeToString(bytes)
    }

    companion object {
        private val ACCESS_TOKEN_EXPIRY: Duration = Duration.ofMinutes(15)
        private val REFRESH_TOKEN_EXPIRY: Duration = Duration.ofDays(30)
        private const val REFRESH_TOKEN_BYTES = 32
    }
}
