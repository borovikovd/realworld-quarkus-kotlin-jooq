package com.example.common.web

import com.example.common.security.CurrentUser
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.container.ResourceInfo
import jakarta.ws.rs.ext.Provider
import org.jboss.resteasy.reactive.ResponseStatus
import org.jboss.resteasy.reactive.server.ServerResponseFilter
import org.slf4j.MDC
import java.util.UUID

private const val OK_STATUS = 200

@ApplicationScoped
class StatusCodeResponseFilter {
    @Suppress("SpreadOperator")
    @ServerResponseFilter
    fun adjustStatusCode(
        responseContext: ContainerResponseContext,
        resourceInfo: ResourceInfo,
    ) {
        if (responseContext.status != OK_STATUS) return
        val status =
            runCatching {
                resourceInfo.resourceClass
                    .getMethod(resourceInfo.resourceMethod.name, *resourceInfo.resourceMethod.parameterTypes)
                    .getAnnotation(ResponseStatus::class.java)
            }.getOrNull() ?: return
        responseContext.status = status.value
    }
}

@Provider
@ApplicationScoped
class LoggingMdcFilter(
    private val currentUser: CurrentUser,
) : ContainerRequestFilter,
    ContainerResponseFilter {
    override fun filter(requestContext: ContainerRequestContext) {
        val requestId = UUID.randomUUID().toString()
        MDC.put("requestId", requestId)
        requestContext.setProperty(REQUEST_ID_PROP, requestId)
        currentUser.id?.let { MDC.put("userId", it.value.toString()) }
    }

    override fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext,
    ) {
        val requestId = requestContext.getProperty(REQUEST_ID_PROP) as? String
        if (requestId != null) responseContext.headers.putSingle("X-Request-Id", requestId)
        MDC.clear()
    }

    companion object {
        private const val REQUEST_ID_PROP = "requestId"
    }
}
