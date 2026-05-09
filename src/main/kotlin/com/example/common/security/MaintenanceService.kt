package com.example.common.security

import com.example.common.time.Clock
import com.example.common.web.IdempotencyRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.temporal.ChronoUnit

@ApplicationScoped
class MaintenanceService(
    private val tokenIssuer: TokenIssuer,
    private val idempotencyRepository: IdempotencyRepository,
    private val clock: Clock,
) {
    @Transactional
    fun cleanupExpiredRefreshTokens(): Int {
        val cutoff = clock.now().minus(1, ChronoUnit.DAYS)
        return tokenIssuer.purgeExpiredRefreshTokens(cutoff)
    }

    @Transactional
    fun cleanupExpiredIdempotencyKeys(): Int = idempotencyRepository.deleteExpiredBefore(clock.now())

    @Transactional
    fun cleanupExpiredRevokedTokens(): Int = tokenIssuer.purgeExpiredAccessTokenRevocations(clock.now())
}
