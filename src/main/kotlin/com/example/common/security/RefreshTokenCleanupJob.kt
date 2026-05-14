package com.example.common.security

import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@ApplicationScoped
class RefreshTokenCleanupJob(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val revokedTokenRepository: RevokedTokenRepository,
) {
    @Scheduled(every = "24h", delayed = "1h")
    fun deleteExpired() {
        runStep("refresh tokens") { purgeRefreshTokens() }
        runStep("revoked tokens") { purgeRevokedTokens() }
    }

    @Transactional
    fun purgeRefreshTokens(): Int =
        refreshTokenRepository.deleteExpiredBefore(OffsetDateTime.now().minus(1, ChronoUnit.DAYS))

    @Transactional
    fun purgeRevokedTokens(): Int = revokedTokenRepository.deleteExpiredBefore(OffsetDateTime.now())

    private fun runStep(
        name: String,
        block: () -> Int,
    ) {
        runCatching(block)
            .onSuccess { log.info("Maintenance: deleted {} {}", it, name) }
            .onFailure { log.error("Maintenance step failed ({}): {}", name, it.message, it) }
    }

    companion object {
        private val log = LoggerFactory.getLogger(RefreshTokenCleanupJob::class.java)
    }
}
