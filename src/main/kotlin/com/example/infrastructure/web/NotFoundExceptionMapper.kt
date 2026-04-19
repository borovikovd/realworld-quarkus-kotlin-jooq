package com.example.infrastructure.web

import com.example.domain.shared.NotFoundException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
class NotFoundExceptionMapper : ExceptionMapper<NotFoundException> {
    override fun toResponse(exception: NotFoundException): Response =
        Response
            .status(Response.Status.NOT_FOUND)
            .entity(mapOf("errors" to mapOf("body" to listOf(exception.message))))
            .build()
}
