package com.example.infrastructure.security

import com.example.domain.auth.TokenVerifier
import com.example.domain.user.UserId
import io.smallrye.jwt.auth.principal.JWTParser
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class JwtTokenVerifier : TokenVerifier {
    @Inject
    lateinit var parser: JWTParser

    override fun verify(token: String): UserId? =
        runCatching { parser.parse(token) }
            .getOrNull()
            ?.subject
            ?.toLongOrNull()
            ?.let { UserId(it) }
}
