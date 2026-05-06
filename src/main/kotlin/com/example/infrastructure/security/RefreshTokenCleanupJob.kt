package com.example.infrastructure.security

import com.example.application.usecase.MaintenanceCommands
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import org.slf4j.LoggerFactory

@ApplicationScoped
class RefreshTokenCleanupJob(
    private val maintenance: MaintenanceCommands,
) {
    @Scheduled(every = "24h", delayed = "1h")
    fun deleteExpired() {
        val deletedTokens = runStep("refresh tokens") { maintenance.cleanupExpiredRefreshTokens() }
        val deletedKeys = runStep("idempotency keys") { maintenance.cleanupExpiredIdempotencyKeys() }
        val deletedRevoked = runStep("revoked tokens") { maintenance.cleanupExpiredRevokedTokens() }
        log.info(
            "Maintenance: deleted {} refresh tokens, {} idempotency keys, {} revoked tokens",
            deletedTokens,
            deletedKeys,
            deletedRevoked,
        )
    }

    private fun runStep(
        name: String,
        block: () -> Int,
    ): Int =
        runCatching(block).getOrElse {
            log.error("Maintenance step failed ({}): {}", name, it.message, it)
            -1
        }

    companion object {
        private val log = LoggerFactory.getLogger(RefreshTokenCleanupJob::class.java)
    }
}
