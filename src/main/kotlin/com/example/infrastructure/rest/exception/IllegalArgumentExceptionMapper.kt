package com.example.infrastructure.rest.exception

import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
class IllegalArgumentExceptionMapper : ExceptionMapper<IllegalArgumentException> {
    companion object {
        private const val HTTP_UNPROCESSABLE_ENTITY = 422
    }

    override fun toResponse(exception: IllegalArgumentException): Response =
        Response
            .status(HTTP_UNPROCESSABLE_ENTITY)
            .entity(mapOf("errors" to mapOf("body" to listOf(exception.message ?: "Invalid input"))))
            .build()
}
