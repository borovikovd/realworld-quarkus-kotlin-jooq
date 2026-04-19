package com.example.infrastructure.security

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class RateLimiter(
    private val maxRequests: Int,
    window: Duration,
) {
    companion object {
        private const val MAX_CACHE_SIZE = 10_000L
    }

    private val cache: Cache<String, AtomicInteger> =
        Caffeine
            .newBuilder()
            .expireAfterWrite(window)
            .maximumSize(MAX_CACHE_SIZE)
            .build()

    fun tryAcquire(key: String): Boolean {
        val counter = cache.get(key) { AtomicInteger(0) }
        return counter.incrementAndGet() <= maxRequests
    }
}
