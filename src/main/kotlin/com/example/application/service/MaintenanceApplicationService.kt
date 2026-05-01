package com.example.application.service

import com.example.application.inport.command.MaintenanceCommands
import com.example.application.outport.idempotency.IdempotencyRepository
import com.example.application.outport.security.RefreshTokenRepository
import com.example.application.outport.security.RevokedTokenRepository
import com.example.application.outport.time.Clock
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.temporal.ChronoUnit

@ApplicationScoped
class MaintenanceApplicationService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val idempotencyRepository: IdempotencyRepository,
    private val revokedTokenRepository: RevokedTokenRepository,
    private val clock: Clock,
) : MaintenanceCommands {
    @Transactional
    override fun cleanupExpiredRefreshTokens(): Int {
        val cutoff = clock.now().minus(1, ChronoUnit.DAYS)
        return refreshTokenRepository.deleteExpiredBefore(cutoff)
    }

    @Transactional
    override fun cleanupExpiredIdempotencyKeys(): Int = idempotencyRepository.deleteExpiredBefore(clock.now())

    @Transactional
    override fun cleanupExpiredRevokedTokens(): Int = revokedTokenRepository.deleteExpiredBefore(clock.now())
}
