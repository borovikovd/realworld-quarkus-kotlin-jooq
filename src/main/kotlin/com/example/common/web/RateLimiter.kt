package com.example.common.web

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

internal class RateLimiter(
    private val maxRequests: Int,
    window: Duration,
) {
    private val cache: Cache<String, AtomicInteger> =
        Caffeine
            .newBuilder()
            .expireAfterWrite(window)
            .maximumSize(MAX_CACHE_SIZE)
            .build()

    fun tryAcquire(key: String): Boolean = cache.get(key) { AtomicInteger(0) }.incrementAndGet() <= maxRequests

    companion object {
        private const val MAX_CACHE_SIZE = 10_000L
    }
}
