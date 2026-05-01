package com.example.infrastructure.persistence.jooq.auth

import com.example.application.inport.command.MaintenanceCommands
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
        log.info("Maintenance: deleted {} refresh tokens, {} idempotency keys", deletedTokens, deletedKeys)
    }

    companion object {
        private val log = LoggerFactory.getLogger(RefreshTokenCleanupJob::class.java)
    }
}
