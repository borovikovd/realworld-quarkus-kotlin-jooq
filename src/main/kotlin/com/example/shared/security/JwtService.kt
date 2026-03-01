package com.example.shared.security

import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import java.time.Duration
import java.time.Instant

@ApplicationScoped
class JwtService {
    fun generateToken(
        userId: Long,
        email: String,
        username: String,
    ): String =
        Jwt
            .issuer("https://realworld.io")
            .subject(userId.toString())
            .claim("email", email)
            .claim("username", username)
            .groups(setOf("user"))
            .expiresAt(Instant.now().plus(TOKEN_EXPIRY))
            .sign()

    companion object {
        private val TOKEN_EXPIRY: Duration = Duration.ofDays(60)
    }
}
