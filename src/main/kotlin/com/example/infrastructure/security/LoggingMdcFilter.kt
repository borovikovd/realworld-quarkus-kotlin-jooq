package com.example.infrastructure.security

import com.example.application.port.outbound.CurrentUser
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
    lateinit var currentUser: CurrentUser

    override fun filter(requestContext: ContainerRequestContext) {
        currentUser.id?.let {
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
