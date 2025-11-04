package com.example.shared.security

import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped

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
            .sign()
}
