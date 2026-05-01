package com.example.application.usecase.maintenance

interface MaintenanceCommands {
    fun cleanupExpiredRefreshTokens(): Int

    fun cleanupExpiredIdempotencyKeys(): Int

    fun cleanupExpiredRevokedTokens(): Int
}
