package com.example.infrastructure.security

import com.example.application.outport.TokenIssuer
import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.UserId
import com.example.domain.aggregate.user.Username
import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.time.Duration
import java.time.Instant

@ApplicationScoped
class JwtTokenIssuer(
    @param:ConfigProperty(name = "mp.jwt.verify.issuer") private val issuer: String,
) : TokenIssuer {
    override fun issue(
        userId: UserId,
        email: Email,
        username: Username,
    ): String =
        Jwt
            .issuer(issuer)
            .subject(userId.value.toString())
            .claim("email", email.value)
            .claim("username", username.value)
            .groups(setOf("user"))
            .expiresAt(Instant.now().plus(TOKEN_EXPIRY))
            .sign()

    companion object {
        private val TOKEN_EXPIRY: Duration = Duration.ofDays(60)
    }
}
