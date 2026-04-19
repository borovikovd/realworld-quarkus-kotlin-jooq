package com.example.infrastructure.security

import com.example.application.CurrentUser
import com.example.domain.user.UserId
import jakarta.enterprise.context.RequestScoped
import jakarta.enterprise.inject.Alternative
import org.eclipse.microprofile.jwt.JsonWebToken

@Alternative
@RequestScoped
class JwtCurrentUser(
    private val jwt: JsonWebToken,
) : CurrentUser {
    override val id: UserId?
        get() = jwt.subject?.toLongOrNull()?.let { UserId(it) }
}
