package com.example.infrastructure.security

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RateLimiterTest {
    @Test
    fun `allows requests under limit`() {
        val limiter = RateLimiter(maxRequests = 3, window = Duration.ofMinutes(1))

        assertTrue(limiter.tryAcquire("client-1"))
        assertTrue(limiter.tryAcquire("client-1"))
        assertTrue(limiter.tryAcquire("client-1"))
    }

    @Test
    fun `rejects requests over limit`() {
        val limiter = RateLimiter(maxRequests = 2, window = Duration.ofMinutes(1))

        assertTrue(limiter.tryAcquire("client-1"))
        assertTrue(limiter.tryAcquire("client-1"))
        assertFalse(limiter.tryAcquire("client-1"))
    }

    @Test
    fun `tracks different keys independently`() {
        val limiter = RateLimiter(maxRequests = 1, window = Duration.ofMinutes(1))

        assertTrue(limiter.tryAcquire("client-1"))
        assertFalse(limiter.tryAcquire("client-1"))

        assertTrue(limiter.tryAcquire("client-2"))
        assertFalse(limiter.tryAcquire("client-2"))
    }
}
