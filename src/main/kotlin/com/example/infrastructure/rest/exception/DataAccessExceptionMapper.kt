package com.example.infrastructure.rest.exception

import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.jooq.exception.DataAccessException
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory

@Provider
class DataAccessExceptionMapper : ExceptionMapper<DataAccessException> {
    override fun toResponse(exception: DataAccessException): Response {
        val field = uniqueViolationField(exception)
        return if (field != null) {
            Response
                .status(HTTP_UNPROCESSABLE_ENTITY)
                .entity(mapOf("errors" to mapOf(field to listOf("is already taken"))))
                .build()
        } else {
            logger.error("Unhandled database exception", exception)
            internalError()
        }
    }

    private fun uniqueViolationField(exception: DataAccessException): String? {
        val psql = exception.cause as? PSQLException
        if (psql?.sqlState != UNIQUE_VIOLATION) return null
        val detail = psql.serverErrorMessage?.detail ?: ""
        return when {
            "email_hash" in detail -> "email"
            "username_hash" in detail -> "username"
            else -> null.also { logger.error("Unhandled unique constraint violation", exception) }
        }
    }

    private fun internalError(): Response =
        Response
            .status(Response.Status.INTERNAL_SERVER_ERROR)
            .type("application/problem+json")
            .entity(
                mapOf(
                    "type" to "about:blank",
                    "title" to "Internal Server Error",
                    "status" to Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                    "detail" to "An unexpected error occurred",
                ),
            ).build()

    companion object {
        private const val UNIQUE_VIOLATION = "23505"
        private const val HTTP_UNPROCESSABLE_ENTITY = 422
        private val logger = LoggerFactory.getLogger(DataAccessExceptionMapper::class.java)
    }
}
