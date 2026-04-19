package com.example.infrastructure.web

import com.example.domain.shared.ForbiddenException
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
