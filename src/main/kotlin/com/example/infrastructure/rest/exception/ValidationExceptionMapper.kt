package com.example.infrastructure.rest.exception

import com.example.domain.exception.ValidationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
class ValidationExceptionMapper : ExceptionMapper<ValidationException> {
    companion object {
        private const val HTTP_UNPROCESSABLE_ENTITY = 422
    }

    override fun toResponse(exception: ValidationException): Response =
        Response
            .status(HTTP_UNPROCESSABLE_ENTITY)
            .entity(mapOf("errors" to exception.errors))
            .build()
}
