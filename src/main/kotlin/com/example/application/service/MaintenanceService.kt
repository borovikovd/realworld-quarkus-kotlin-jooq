package com.example.application.service

import com.example.application.port.Clock
import com.example.application.port.IdempotencyRepository
import com.example.application.port.security.TokenIssuer
import com.example.application.usecase.MaintenanceCommands
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.temporal.ChronoUnit

@ApplicationScoped
class MaintenanceService(
    private val tokenIssuer: TokenIssuer,
    private val idempotencyRepository: IdempotencyRepository,
    private val clock: Clock,
) : MaintenanceCommands {
    @Transactional
    override fun cleanupExpiredRefreshTokens(): Int {
        val cutoff = clock.now().minus(1, ChronoUnit.DAYS)
        return tokenIssuer.purgeExpiredRefreshTokens(cutoff)
    }

    @Transactional
    override fun cleanupExpiredIdempotencyKeys(): Int = idempotencyRepository.deleteExpiredBefore(clock.now())

    @Transactional
    override fun cleanupExpiredRevokedTokens(): Int = tokenIssuer.purgeExpiredAccessTokenRevocations(clock.now())
}
