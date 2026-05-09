package com.example.common.security

import com.example.common.web.UnauthorizedException
import com.example.user.UserId
import jakarta.enterprise.context.RequestScoped
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.UUID

@RequestScoped
class CurrentUser(
    private val jwt: JsonWebToken,
) {
    val id: UserId?
        get() = jwt.subject?.toLongOrNull()?.let { UserId(it) }

    val jti: UUID?
        get() = jwt.tokenID?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    val rawToken: String?
        get() = jwt.rawToken

    fun require(): UserId = id ?: throw UnauthorizedException("Authentication required")
}
