package com.example.infrastructure.rest.filter

import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ResourceInfo
import org.jboss.resteasy.reactive.ResponseStatus
import org.jboss.resteasy.reactive.server.ServerResponseFilter

private const val OK_STATUS = 200

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
