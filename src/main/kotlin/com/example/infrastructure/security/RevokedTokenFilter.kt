package com.example.infrastructure.security

import com.example.application.port.security.RevokedTokenRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.UUID

@Provider
@ApplicationScoped
class RevokedTokenFilter(
    private val jwt: JsonWebToken,
    private val revokedTokenRepository: RevokedTokenRepository,
) : ContainerRequestFilter {
    override fun filter(requestContext: ContainerRequestContext) {
        val jti = resolveJti(requestContext) ?: return
        if (revokedTokenRepository.isRevoked(jti)) {
            requestContext.abortWith(
                Response
                    .status(Response.Status.UNAUTHORIZED)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(mapOf("errors" to mapOf("token" to listOf("has been revoked"))))
                    .build(),
            )
        }
    }

    private fun resolveJti(requestContext: ContainerRequestContext): UUID? =
        if (!requestContext.securityContext.isUserInRole("user")) {
            null
        } else {
            jwt.tokenID?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        }
}
