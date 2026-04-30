package com.example.application.inport.command

interface MaintenanceCommands {
    fun cleanupExpiredRefreshTokens(): Int
}
