package com.example.common.web

import com.example.common.validation.ValidationException
import jakarta.validation.ConstraintViolationException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.jooq.exception.DataAccessException
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory

private const val HTTP_422 = 422
private const val PROBLEM_JSON = "application/problem+json"

private fun internalErrorResponse(): Response =
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
            .entity(mapOf("errors" to mapOf(exception.field to listOf("forbidden"))))
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
class InvalidCredentialsExceptionMapper : ExceptionMapper<InvalidCredentialsException> {
    override fun toResponse(exception: InvalidCredentialsException): Response =
        Response
            .status(Response.Status.UNAUTHORIZED)
            .entity(mapOf("errors" to mapOf("credentials" to listOf("invalid"))))
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
            .status(HTTP_422)
            .entity(mapOf("errors" to exception.errors))
            .build()
}

@Provider
class ConstraintViolationExceptionMapper : ExceptionMapper<ConstraintViolationException> {
    override fun toResponse(exception: ConstraintViolationException): Response {
        val errors =
            exception.constraintViolations
                .groupBy { v -> v.propertyPath.last().name }
                .mapValues { (_, vs) -> vs.map { it.message } }
        return Response
            .status(HTTP_422)
            .entity(mapOf("errors" to errors))
            .build()
    }
}

@Provider
class IllegalArgumentExceptionMapper : ExceptionMapper<IllegalArgumentException> {
    override fun toResponse(exception: IllegalArgumentException): Response =
        Response
            .status(HTTP_422)
            .entity(mapOf("errors" to mapOf("body" to listOf(exception.message ?: "Invalid input"))))
            .build()
}

@Provider
class DataAccessExceptionMapper : ExceptionMapper<DataAccessException> {
    override fun toResponse(exception: DataAccessException): Response {
        val field = uniqueViolationField(exception)
        return if (field != null) {
            Response
                .status(HTTP_422)
                .entity(mapOf("errors" to mapOf(field to listOf("has already been taken"))))
                .build()
        } else {
            logger.error("Unhandled database exception", exception)
            internalErrorResponse()
        }
    }

    private fun uniqueViolationField(exception: DataAccessException): String? {
        val psql = exception.cause as? PSQLException
        if (psql?.sqlState != UNIQUE_VIOLATION) return null
        return when (psql.serverErrorMessage?.constraint) {
            "user_email_key" -> "email"
            "user_username_key" -> "username"
            "articles_slug_key" -> "slug"
            else -> null.also { logger.error("Unhandled unique constraint violation", exception) }
        }
    }

    companion object {
        private const val UNIQUE_VIOLATION = "23505"
        private val logger = LoggerFactory.getLogger(DataAccessExceptionMapper::class.java)
    }
}

@Provider
class UnhandledExceptionMapper : ExceptionMapper<Exception> {
    override fun toResponse(exception: Exception): Response {
        if (exception is WebApplicationException) return exception.response
        logger.error("Unhandled exception", exception)
        return internalErrorResponse()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UnhandledExceptionMapper::class.java)
    }
}
