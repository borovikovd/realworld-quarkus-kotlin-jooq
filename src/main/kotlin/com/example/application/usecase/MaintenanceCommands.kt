package com.example.application.usecase

interface MaintenanceCommands {
    fun cleanupExpiredRefreshTokens(): Int

    fun cleanupExpiredIdempotencyKeys(): Int

    fun cleanupExpiredRevokedTokens(): Int
}
