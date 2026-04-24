package com.example.infrastructure.security

import com.example.application.outport.CurrentUser
import com.example.domain.aggregate.user.UserId
import jakarta.enterprise.context.RequestScoped
import org.eclipse.microprofile.jwt.JsonWebToken

@RequestScoped
class JwtCurrentUser(
    private val jwt: JsonWebToken,
) : CurrentUser {
    override val id: UserId?
        get() = jwt.subject?.toLongOrNull()?.let { UserId(it) }
}
