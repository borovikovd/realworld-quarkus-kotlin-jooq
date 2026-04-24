package com.example.infrastructure.rest.exception

import com.example.domain.exception.ForbiddenException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
class ForbiddenExceptionMapper : ExceptionMapper<ForbiddenException> {
    override fun toResponse(exception: ForbiddenException): Response =
        Response
            .status(Response.Status.FORBIDDEN)
            .entity(mapOf("errors" to mapOf("body" to listOf(exception.message))))
            .build()
}
