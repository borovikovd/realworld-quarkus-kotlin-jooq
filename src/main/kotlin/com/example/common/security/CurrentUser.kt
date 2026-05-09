package com.example.common.security

import com.example.common.web.UnauthorizedException
import com.example.user.UserId
import jakarta.enterprise.context.RequestScoped
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.UUID

interface CurrentUser {
    val id: UserId?
    val jti: UUID?
    val rawToken: String?

    fun require(): UserId = id ?: throw UnauthorizedException("Authentication required")
}

@RequestScoped
class JwtCurrentUser(
    private val jwt: JsonWebToken,
) : CurrentUser {
    override val id: UserId?
        get() = jwt.subject?.toLongOrNull()?.let { UserId(it) }

    override val jti: UUID?
        get() = jwt.tokenID?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    override val rawToken: String?
        get() = jwt.rawToken
}
