package com.example.common.security

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider

@Provider
@ApplicationScoped
class RevokedTokenFilter(
    private val currentUser: CurrentUser,
    private val revokedTokenRepository: RevokedTokenRepository,
) : ContainerRequestFilter {
    override fun filter(requestContext: ContainerRequestContext) {
        val jti = currentUser.jti ?: return
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
}
