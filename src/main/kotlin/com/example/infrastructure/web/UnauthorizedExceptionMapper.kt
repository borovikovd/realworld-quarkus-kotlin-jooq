package com.example.infrastructure.web

import com.example.domain.shared.UnauthorizedException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
class UnauthorizedExceptionMapper : ExceptionMapper<UnauthorizedException> {
    override fun toResponse(exception: UnauthorizedException): Response =
        Response
            .status(Response.Status.UNAUTHORIZED)
            .entity(mapOf("errors" to mapOf("body" to listOf(exception.message))))
            .build()
}
