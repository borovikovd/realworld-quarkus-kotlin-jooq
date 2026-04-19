package com.example.shared.security

import com.example.domain.shared.UnauthorizedException
import com.example.user.domain.UserId
import jakarta.enterprise.context.RequestScoped
import org.eclipse.microprofile.jwt.JsonWebToken

@RequestScoped
class SecurityContext(
    private val jwt: JsonWebToken,
) {
    val currentUserId: UserId?
        get() = jwt.subject?.toLongOrNull()?.let { UserId(it) }

    val isAuthenticated: Boolean
        get() = currentUserId != null

    fun requireCurrentUserId(): UserId = currentUserId ?: throw UnauthorizedException("Authentication required")
}
