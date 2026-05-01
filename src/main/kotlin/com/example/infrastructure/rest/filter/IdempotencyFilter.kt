@file:Suppress("TooGenericExceptionCaught")

package com.example.infrastructure.rest.filter

import com.example.application.port.Clock
import com.example.application.port.IdempotencyRepository
import com.example.application.port.security.CurrentUser
import com.example.application.readmodel.StoredIdempotencyKey
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.StreamingOutput
import jakarta.ws.rs.ext.Provider
import org.slf4j.LoggerFactory

@Provider
@ApplicationScoped
class IdempotencyFilter(
    private val idempotencyRepository: IdempotencyRepository,
    private val currentUser: CurrentUser,
    private val clock: Clock,
    private val objectMapper: ObjectMapper,
) : ContainerRequestFilter,
    ContainerResponseFilter {
    override fun filter(requestContext: ContainerRequestContext) {
        val key = requestContext.getHeaderString(IDEMPOTENCY_KEY_HEADER) ?: return
        if (requestContext.method != "POST") return
        try {
            handleRequest(requestContext, key)
        } catch (e: Exception) {
            log.warn("Idempotency check failed, proceeding without guarantee: {}", e.message)
        }
    }

    override fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext,
    ) {
        val key = requestContext.getProperty(KEY_PROP) as? String ?: return
        val scope = requestContext.getProperty(SCOPE_PROP) as? String ?: return
        try {
            handleResponse(key, scope, responseContext)
        } catch (e: Exception) {
            log.warn("Idempotency response storage failed: {}", e.message)
            idempotencyRepository.deleteByKeyAndScope(key, scope)
        }
    }

    private fun handleRequest(
        ctx: ContainerRequestContext,
        key: String,
    ) {
        val scope = currentUser.id?.value?.toString() ?: ANON_SCOPE
        val path = ctx.uriInfo.requestUri.path

        val existing = idempotencyRepository.findByKeyAndScope(key, scope)
        if (existing != null) {
            ctx.abortWith(responseFor(existing, path))
            return
        }

        val expiry = clock.now().plusHours(KEY_EXPIRY_HOURS)
        val inserted = idempotencyRepository.insertProcessing(key, scope, path, expiry)
        if (inserted) {
            ctx.setProperty(KEY_PROP, key)
            ctx.setProperty(SCOPE_PROP, scope)
        } else {
            // Race — another request inserted between our read and insert
            val raced = idempotencyRepository.findByKeyAndScope(key, scope)
            if (raced != null) ctx.abortWith(responseFor(raced, path))
        }
    }

    private fun handleResponse(
        key: String,
        scope: String,
        responseContext: ContainerResponseContext,
    ) {
        val status = responseContext.status
        if (status >= HTTP_SERVER_ERROR) {
            idempotencyRepository.deleteByKeyAndScope(key, scope)
            return
        }
        val body = responseContext.entity?.let { objectMapper.writeValueAsString(it) } ?: ""
        idempotencyRepository.complete(key, scope, status, body)
    }

    private fun responseFor(
        stored: StoredIdempotencyKey,
        currentPath: String,
    ): Response =
        when {
            stored.requestPath != currentPath ->
                Response
                    .status(HTTP_UNPROCESSABLE_ENTITY)
                    .entity(mapOf("errors" to mapOf("idempotencyKey" to listOf(ERR_WRONG_ENDPOINT))))
                    .build()
            stored.isProcessing ->
                Response
                    .status(HTTP_CONFLICT)
                    .header("Retry-After", "1")
                    .entity(mapOf("errors" to mapOf("idempotencyKey" to listOf(ERR_IN_PROGRESS))))
                    .build()
            else -> {
                val body = stored.responseBody!!
                Response
                    .status(stored.responseStatus!!)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(StreamingOutput { it.write(body.toByteArray(Charsets.UTF_8)) })
                    .build()
            }
        }

    companion object {
        const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"
        private const val KEY_PROP = "idempotency.key"
        private const val SCOPE_PROP = "idempotency.scope"
        private const val ANON_SCOPE = "anon"
        private const val KEY_EXPIRY_HOURS = 24L
        private const val HTTP_CONFLICT = 409
        private const val HTTP_UNPROCESSABLE_ENTITY = 422
        private const val HTTP_SERVER_ERROR = 500
        private const val ERR_WRONG_ENDPOINT = "was already used for a different endpoint"
        private const val ERR_IN_PROGRESS = "a request with this key is still being processed"
        private val log = LoggerFactory.getLogger(IdempotencyFilter::class.java)
    }
}
