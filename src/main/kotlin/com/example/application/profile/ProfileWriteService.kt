package com.example.application.profile

interface ProfileWriteService {
    fun followUser(username: String)

    fun unfollowUser(username: String)
}
