package com.example.application.user

interface UserReadService {
    fun hydrate(id: Long): UserSummary
}
