package com.example.common.web

import jakarta.validation.ConstraintViolationException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.jooq.exception.DataAccessException
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory

@Provider
class SecurityUnauthorizedExceptionMapper : ExceptionMapper<io.quarkus.security.UnauthorizedException> {
    override fun toResponse(exception: io.quarkus.security.UnauthorizedException): Response =
        Response
            .status(Response.Status.UNAUTHORIZED)
            .entity(mapOf("errors" to mapOf("token" to listOf("is missing"))))
            .build()
}

@Provider
class SecurityForbiddenExceptionMapper : ExceptionMapper<io.quarkus.security.ForbiddenException> {
    override fun toResponse(exception: io.quarkus.security.ForbiddenException): Response =
        Response
            .status(Response.Status.FORBIDDEN)
            .entity(mapOf("errors" to mapOf("token" to listOf("is missing"))))
            .build()
}

@Provider
class NotFoundExceptionMapper : ExceptionMapper<NotFoundException> {
    override fun toResponse(exception: NotFoundException): Response =
        Response
            .status(Response.Status.NOT_FOUND)
            .entity(mapOf("errors" to mapOf(exception.field to listOf("not found"))))
            .build()
}

@Provider
class ForbiddenExceptionMapper : ExceptionMapper<ForbiddenException> {
    override fun toResponse(exception: ForbiddenException): Response =
        Response
            .status(Response.Status.FORBIDDEN)
            .entity(mapOf("errors" to mapOf("body" to listOf(exception.message))))
            .build()
}

@Provider
class UnauthorizedExceptionMapper : ExceptionMapper<UnauthorizedException> {
    override fun toResponse(exception: UnauthorizedException): Response =
        Response
            .status(Response.Status.UNAUTHORIZED)
            .entity(mapOf("errors" to mapOf("body" to listOf(exception.message))))
            .build()
}

@Provider
class ConflictExceptionMapper : ExceptionMapper<ConflictException> {
    override fun toResponse(exception: ConflictException): Response =
        Response
            .status(Response.Status.CONFLICT)
            .entity(mapOf("errors" to exception.errors))
            .build()
}

@Provider
class ValidationExceptionMapper : ExceptionMapper<ValidationException> {
    override fun toResponse(exception: ValidationException): Response =
        Response
            .status(HTTP_UNPROCESSABLE_ENTITY)
            .entity(mapOf("errors" to exception.errors))
            .build()

    companion object {
        private const val HTTP_UNPROCESSABLE_ENTITY = 422
    }
}

@Provider
class ConstraintViolationExceptionMapper : ExceptionMapper<ConstraintViolationException> {
    override fun toResponse(exception: ConstraintViolationException): Response {
        val errors =
            exception.constraintViolations
                .groupBy { v -> v.propertyPath.last().name }
                .mapValues { (_, vs) -> vs.map { it.message } }
        return Response
            .status(HTTP_UNPROCESSABLE_ENTITY)
            .entity(mapOf("errors" to errors))
            .build()
    }

    companion object {
        private const val HTTP_UNPROCESSABLE_ENTITY = 422
    }
}

@Provider
class IllegalArgumentExceptionMapper : ExceptionMapper<IllegalArgumentException> {
    override fun toResponse(exception: IllegalArgumentException): Response =
        Response
            .status(HTTP_UNPROCESSABLE_ENTITY)
            .entity(mapOf("errors" to mapOf("body" to listOf(exception.message ?: "Invalid input"))))
            .build()

    companion object {
        private const val HTTP_UNPROCESSABLE_ENTITY = 422
    }
}

@Provider
class DataAccessExceptionMapper : ExceptionMapper<DataAccessException> {
    override fun toResponse(exception: DataAccessException): Response {
        val field = uniqueViolationField(exception)
        return if (field != null) {
            Response
                .status(HTTP_UNPROCESSABLE_ENTITY)
                .entity(mapOf("errors" to mapOf(field to listOf("has already been taken"))))
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
            "articles_slug_key" in detail -> "slug"
            else -> null.also { logger.error("Unhandled unique constraint violation", exception) }
        }
    }

    private fun internalError(): Response =
        Response
            .status(Response.Status.INTERNAL_SERVER_ERROR)
            .type(PROBLEM_JSON)
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
        private const val PROBLEM_JSON = "application/problem+json"
        private val logger = LoggerFactory.getLogger(DataAccessExceptionMapper::class.java)
    }
}

@Provider
class UnhandledExceptionMapper : ExceptionMapper<Exception> {
    override fun toResponse(exception: Exception): Response {
        if (exception is WebApplicationException) return exception.response
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

    companion object {
        private val logger = LoggerFactory.getLogger(UnhandledExceptionMapper::class.java)
        private const val PROBLEM_JSON = "application/problem+json"
        private const val INTERNAL_SERVER_ERROR = 500
    }
}
