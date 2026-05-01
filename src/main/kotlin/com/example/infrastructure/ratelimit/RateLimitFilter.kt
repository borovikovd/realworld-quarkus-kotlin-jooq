package com.example.infrastructure.ratelimit

import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory
import java.time.Duration

@Provider
@ApplicationScoped
class RateLimitFilter(
    private val routingContext: RoutingContext,
    @param:ConfigProperty(name = "rate-limit.trusted-proxy-count", defaultValue = "0")
    private val trustedProxyCount: Int,
    @ConfigProperty(name = "rate-limit.login.max-requests", defaultValue = "10")
    loginMaxRequests: Int,
    @ConfigProperty(name = "rate-limit.registration.max-requests", defaultValue = "3")
    registrationMaxRequests: Int,
    @ConfigProperty(name = "rate-limit.window-seconds", defaultValue = "60")
    windowSeconds: Long,
) : ContainerRequestFilter {
    private val window = Duration.ofSeconds(windowSeconds)
    private val loginLimiter = RateLimiter(loginMaxRequests, window).takeIf { loginMaxRequests > 0 }
    private val registrationLimiter =
        RateLimiter(registrationMaxRequests, window).takeIf { registrationMaxRequests > 0 }

    companion object {
        private val logger = LoggerFactory.getLogger(RateLimitFilter::class.java)
        private const val TOO_MANY_REQUESTS = 429
        private const val PROBLEM_JSON = "application/problem+json"
    }

    override fun filter(requestContext: ContainerRequestContext) {
        if (requestContext.method != "POST") return

        val path =
            requestContext.uriInfo.path
                .trim('/')
                .lowercase()
        val limiter = limiterFor(path) ?: return

        val clientIp = extractClientIp(requestContext)
        if (!limiter.tryAcquire(clientIp)) {
            logger.warn("Rate limit exceeded: path={}, ip={}", path, clientIp)
            requestContext.abortWith(tooManyRequestsResponse())
        }
    }

    private fun limiterFor(path: String): RateLimiter? =
        when (path) {
            "users/login" -> loginLimiter
            "users" -> registrationLimiter
            else -> null
        }

    private fun tooManyRequestsResponse(): Response =
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
            ).build()

    private fun extractClientIp(requestContext: ContainerRequestContext): String {
        val remoteAddress = routingContext.request().remoteAddress()?.host() ?: "unknown"
        val forwarded =
            requestContext
                .getHeaderString("X-Forwarded-For")
                .takeIf { trustedProxyCount > 0 && !it.isNullOrBlank() }
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }

        // Each trusted proxy appends the IP it received the connection from (left-to-right).
        // With N trusted proxies the rightmost N entries are proxy-controlled; the entry just
        // before them is the actual client IP that the outermost trusted proxy observed.
        return forwarded?.getOrNull(forwarded.size - trustedProxyCount) ?: remoteAddress
    }
}
