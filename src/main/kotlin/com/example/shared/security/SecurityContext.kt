package com.example.shared.security

import com.example.shared.exceptions.UnauthorizedException
import jakarta.enterprise.context.RequestScoped
import org.eclipse.microprofile.jwt.JsonWebToken

@RequestScoped
class SecurityContext(
    private val jwt: JsonWebToken,
) {
    val currentUserId: Long?
        get() = if (jwt.subject != null) jwt.subject.toLongOrNull() else null

    val isAuthenticated: Boolean
        get() = currentUserId != null

    fun requireCurrentUserId(): Long = currentUserId ?: throw UnauthorizedException("Authentication required")
}
