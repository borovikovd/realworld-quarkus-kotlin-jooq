package com.example.application.usecase.profile

interface ProfileCommands {
    fun followUser(username: String)

    fun unfollowUser(username: String)
}
