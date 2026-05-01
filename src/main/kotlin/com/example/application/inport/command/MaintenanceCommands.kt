package com.example.application.inport.command

interface MaintenanceCommands {
    fun cleanupExpiredRefreshTokens(): Int

    fun cleanupExpiredIdempotencyKeys(): Int

    fun cleanupExpiredRevokedTokens(): Int
}
