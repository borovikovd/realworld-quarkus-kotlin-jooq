package com.example.infrastructure.security

import com.example.shared.security.SecurityContext
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider
import org.slf4j.MDC

@Provider
@ApplicationScoped
class LoggingMdcFilter :
    ContainerRequestFilter,
    ContainerResponseFilter {
    @Inject
    lateinit var securityContext: SecurityContext

    override fun filter(requestContext: ContainerRequestContext) {
        securityContext.currentUserId?.let {
            MDC.put("userId", it.value.toString())
        }
    }

    override fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext,
    ) {
        MDC.clear()
    }
}
