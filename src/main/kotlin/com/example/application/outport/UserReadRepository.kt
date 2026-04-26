package com.example.application.outport

import com.example.application.readmodel.LoginCredentials
import com.example.application.readmodel.UserReadModel
import com.example.domain.aggregate.user.Email
import com.example.domain.aggregate.user.UserId

interface UserReadRepository {
    fun findById(id: Long): UserReadModel?

    fun findCredentialsByEmail(email: Email): LoginCredentials?

    fun findUserIdByUsername(username: String): UserId?
}
