package com.example.infrastructure.security

import com.example.application.port.security.CurrentUser
import com.example.domain.aggregate.user.UserId
import jakarta.enterprise.context.RequestScoped
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.UUID

@RequestScoped
class JwtCurrentUser(
    private val jwt: JsonWebToken,
) : CurrentUser {
    override val id: UserId?
        get() = jwt.subject?.toLongOrNull()?.let { UserId(it) }

    override val jti: UUID?
        get() = jwt.tokenID?.let { runCatching { UUID.fromString(it) }.getOrNull() }
}
