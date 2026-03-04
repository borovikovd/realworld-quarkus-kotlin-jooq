package com.example.shared.security

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.slf4j.LoggerFactory
import java.time.Duration

@Provider
@ApplicationScoped
class RateLimitFilter : ContainerRequestFilter {
    companion object {
        private val logger = LoggerFactory.getLogger(RateLimitFilter::class.java)
        private const val TOO_MANY_REQUESTS = 429
        private const val PROBLEM_JSON = "application/problem+json"
    }

    private val loginLimiter = RateLimiter(maxRequests = 10, window = Duration.ofMinutes(1))
    private val registrationLimiter = RateLimiter(maxRequests = 3, window = Duration.ofMinutes(1))

    override fun filter(requestContext: ContainerRequestContext) {
        if (requestContext.method != "POST") return

        val path = requestContext.uriInfo.path
        val limiter =
            when {
                path == "users/login" -> loginLimiter
                path == "users" -> registrationLimiter
                else -> return
            }

        val clientIp = extractClientIp(requestContext)
        if (!limiter.tryAcquire(clientIp)) {
            logger.warn("Rate limit exceeded: path={}, ip={}", path, clientIp)
            requestContext.abortWith(
                Response
                    .status(TOO_MANY_REQUESTS)
                    .type(PROBLEM_JSON)
                    .header("Retry-After", "60")
                    .entity(
                        mapOf(
                            "type" to "about:blank",
                            "title" to "Too Many Requests",
                            "status" to TOO_MANY_REQUESTS,
                            "detail" to "Rate limit exceeded",
                        ),
                    ).build(),
            )
        }
    }

    private fun extractClientIp(requestContext: ContainerRequestContext): String =
        requestContext
            .getHeaderString("X-Forwarded-For")
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?: requestContext.getHeaderString("X-Real-IP")
            ?: "unknown"
}
