package com.example.profile.application

interface ProfileWriteService {
    fun followUser(username: String)

    fun unfollowUser(username: String)
}
