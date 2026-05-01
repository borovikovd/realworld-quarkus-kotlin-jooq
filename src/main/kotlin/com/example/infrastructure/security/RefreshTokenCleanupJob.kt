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
        val deletedTokens = maintenance.cleanupExpiredRefreshTokens()
        val deletedKeys = maintenance.cleanupExpiredIdempotencyKeys()
        val deletedRevoked = maintenance.cleanupExpiredRevokedTokens()
        log.info(
            "Maintenance: deleted {} refresh tokens, {} idempotency keys, {} revoked tokens",
            deletedTokens,
            deletedKeys,
            deletedRevoked,
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(RefreshTokenCleanupJob::class.java)
    }
}
