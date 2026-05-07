package com.example.application.port

import com.example.application.readmodel.LoginCredentials
import com.example.application.readmodel.UserReadModel
import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.UserId

interface UserFinder {
    fun findReadModelById(id: UserId): UserReadModel?

    fun findCredentialsByEmail(email: Email): LoginCredentials?

    fun findUserIdByUsername(username: String): UserId?
}
