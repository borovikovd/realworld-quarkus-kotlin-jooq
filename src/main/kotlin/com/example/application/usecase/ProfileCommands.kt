package com.example.application.usecase

interface ProfileCommands {
    fun followUser(username: String)

    fun unfollowUser(username: String)
}
