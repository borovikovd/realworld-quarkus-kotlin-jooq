package com.example.user.application

interface UserReadService {
    fun hydrate(id: Long): UserSummary
}
