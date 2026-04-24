package com.example.application.inport.command

interface ProfileCommands {
    fun followUser(username: String)

    fun unfollowUser(username: String)
}
