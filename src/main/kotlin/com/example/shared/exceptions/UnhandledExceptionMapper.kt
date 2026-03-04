package com.example.shared.exceptions

import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.slf4j.LoggerFactory

@Provider
class UnhandledExceptionMapper : ExceptionMapper<Exception> {
    companion object {
        private val logger = LoggerFactory.getLogger(UnhandledExceptionMapper::class.java)
        private const val PROBLEM_JSON = "application/problem+json"
        private const val INTERNAL_SERVER_ERROR = 500
    }

    override fun toResponse(exception: Exception): Response {
        logger.error("Unhandled exception", exception)
        return Response
            .status(Response.Status.INTERNAL_SERVER_ERROR)
            .type(PROBLEM_JSON)
            .entity(
                mapOf(
                    "type" to "about:blank",
                    "title" to "Internal Server Error",
                    "status" to INTERNAL_SERVER_ERROR,
                    "detail" to "An unexpected error occurred",
                ),
            ).build()
    }
}
